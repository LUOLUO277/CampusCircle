SELECT column_name
FROM information_schema.columns
WHERE table_schema = 'campus_circle_cy_db'
  AND table_name = 'user_notice_status'
ORDER BY ordinal_position;
