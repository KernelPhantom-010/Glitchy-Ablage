-- =====================================================================
-- Glitchy – Supabase Schema
-- Ausführen: Supabase Dashboard -> SQL Editor -> New query -> einfügen -> Run
-- =====================================================================

create extension if not exists citext with schema extensions;

-- ---------- Profile ----------
create table if not exists public.profiles (
  id          uuid primary key references auth.users(id) on delete cascade,
  username    extensions.citext unique
              check (username is null or username::text ~ '^[A-Za-z0-9_]{3,20}$'),
  avatar_url  text,
  created_at  timestamptz not null default now()
);
alter table public.profiles enable row level security;

-- Username + Avatar sind öffentlich lesbar (Leaderboards). KEINE E-Mail in dieser Tabelle!
create policy "profiles public read" on public.profiles
  for select using (true);
create policy "profiles own update" on public.profiles
  for update to authenticated using (auth.uid() = id) with check (auth.uid() = id);

-- Profil automatisch beim Registrieren anlegen (Username kommt aus den Signup-Metadaten)
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, username)
  values (new.id, nullif(new.raw_user_meta_data->>'username', ''));
  return new;
end $$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Ist ein Username noch frei? (auch ohne Login aufrufbar, z.B. beim Registrieren)
create or replace function public.username_free(p_name text)
returns boolean language sql security definer set search_path = public stable as $$
  select not exists (select 1 from public.profiles where username = p_name::extensions.citext);
$$;
grant execute on function public.username_free(text) to anon, authenticated;

-- ---------- Lernfortschritt ----------
create table if not exists public.lesson_progress (
  user_id    uuid not null default auth.uid() references auth.users(id) on delete cascade,
  lesson_id  text not null,
  done_at    timestamptz not null default now(),
  primary key (user_id, lesson_id)
);
alter table public.lesson_progress enable row level security;
create policy "progress own select" on public.lesson_progress for select to authenticated using (auth.uid() = user_id);
create policy "progress own insert" on public.lesson_progress for insert to authenticated with check (auth.uid() = user_id);
create policy "progress own delete" on public.lesson_progress for delete to authenticated using (auth.uid() = user_id);

-- ---------- Testergebnisse ----------
create table if not exists public.test_results (
  id         bigint generated always as identity primary key,
  user_id    uuid not null default auth.uid() references auth.users(id) on delete cascade,
  module_id  text not null,
  score      int  not null check (score between 0 and 100),
  created_at timestamptz not null default now()
);
alter table public.test_results enable row level security;
create policy "tests own select" on public.test_results for select to authenticated using (auth.uid() = user_id);
create policy "tests own insert" on public.test_results for insert to authenticated with check (auth.uid() = user_id);

-- ---------- Tägliche Aktivität (für die Leaderboards) ----------
create table if not exists public.activity (
  user_id    uuid not null references auth.users(id) on delete cascade,
  day        date not null,
  solved     boolean not null default false,   -- mind. 1 Aufgabe gelöst
  challenge  boolean not null default false,   -- Daily Challenge geschafft
  primary key (user_id, day)
);
alter table public.activity enable row level security;
create policy "activity own select" on public.activity for select to authenticated using (auth.uid() = user_id);
-- Schreiben NUR über die Funktion unten (verhindert direktes Manipulieren der Tabelle)

create or replace function public.log_activity(p_kind text)
returns void language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then raise exception 'not authenticated'; end if;
  if p_kind not in ('solved', 'challenge') then raise exception 'invalid kind'; end if;
  insert into public.activity (user_id, day, solved, challenge)
  values (auth.uid(), (now() at time zone 'Europe/Berlin')::date, p_kind = 'solved', p_kind = 'challenge')
  on conflict (user_id, day) do update
    set solved    = public.activity.solved    or excluded.solved,
        challenge = public.activity.challenge or excluded.challenge;
end $$;
grant execute on function public.log_activity(text) to authenticated;

-- Leaderboard: p_kind = 'activity' | 'challenge', p_days = 7 | 30 | 36500 (Lifetime)
create or replace function public.leaderboard(p_kind text, p_days int)
returns table (username text, avatar_url text, score bigint)
language sql security definer set search_path = public stable as $$
  select p.username::text, p.avatar_url, count(*)::bigint as score
  from public.activity a
  join public.profiles p on p.id = a.user_id
  where p.username is not null
    and a.day > (now() at time zone 'Europe/Berlin')::date - p_days
    and ((p_kind = 'activity' and a.solved) or (p_kind = 'challenge' and a.challenge))
  group by p.username, p.avatar_url
  order by score desc, p.username
  limit 100;
$$;
grant execute on function public.leaderboard(text, int) to anon, authenticated;

-- ---------- Zertifikate ----------
create table if not exists public.certificates (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null default auth.uid() references auth.users(id) on delete cascade,
  module_id  text not null,
  full_name  text not null check (char_length(full_name) between 2 and 80),
  score      int  not null,
  issued_at  timestamptz not null default now()
);
alter table public.certificates enable row level security;
create policy "certs own select" on public.certificates for select to authenticated using (auth.uid() = user_id);
-- Einfügen nur über issue_certificate()

create or replace function public.issue_certificate(p_module text, p_name text)
returns uuid language plpgsql security definer set search_path = public as $$
declare best int; new_id uuid;
begin
  if auth.uid() is null then raise exception 'not authenticated'; end if;
  select max(score) into best from public.test_results where user_id = auth.uid() and module_id = p_module;
  if best is null or best < 95 then raise exception 'score too low'; end if;
  insert into public.certificates (user_id, module_id, full_name, score)
  values (auth.uid(), p_module, trim(p_name), best)
  returning id into new_id;
  return new_id;
end $$;
grant execute on function public.issue_certificate(text, text) to authenticated;

-- Arbeitgeber können ein Zertifikat per ID prüfen (z.B. später über eine kleine Webseite)
create or replace function public.verify_certificate(p_id uuid)
returns table (full_name text, module_id text, score int, issued_at timestamptz)
language sql security definer set search_path = public stable as $$
  select full_name, module_id, score, issued_at from public.certificates where id = p_id;
$$;
grant execute on function public.verify_certificate(uuid) to anon, authenticated;

-- ---------- Melden (Pflicht für Apps mit nutzergenerierten Inhalten, z.B. Profilbilder) ----------
create table if not exists public.reports (
  id          bigint generated always as identity primary key,
  reporter_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  username    text not null,
  reason      text,
  created_at  timestamptz not null default now()
);
alter table public.reports enable row level security;
create policy "reports insert" on public.reports for insert to authenticated with check (auth.uid() = reporter_id);
-- Lesen nur du selbst im Dashboard (service role)

-- ---------- Konto löschen (Pflicht: Google Play + DSGVO) ----------
create or replace function public.delete_my_account()
returns void language plpgsql security definer set search_path = public, auth as $$
begin
  if auth.uid() is null then raise exception 'not authenticated'; end if;
  delete from storage.objects where bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text;
  delete from auth.users where id = auth.uid();   -- Cascade löscht Profil, Fortschritt, Tests, Aktivität
end $$;
grant execute on function public.delete_my_account() to authenticated;

-- ---------- Storage: Profilbilder ----------
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', true, 1048576, array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do nothing;

create policy "avatars public read" on storage.objects
  for select using (bucket_id = 'avatars');
create policy "avatars insert own" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "avatars update own" on storage.objects
  for update to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "avatars delete own" on storage.objects
  for delete to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
