SELECT owner_user_id, COUNT(*) AS cnt
FROM aggregated_notice
GROUP BY owner_user_id
ORDER BY owner_user_id;

SELECT id, source_id, owner_user_id, status, title
FROM aggregated_notice
LIMIT 10;
