SELECT 'subscription_source' AS tbl, COUNT(*) AS cnt FROM subscription_source
UNION ALL
SELECT 'notice_subscription' AS tbl, COUNT(*) AS cnt FROM notice_subscription
UNION ALL
SELECT 'aggregated_notice' AS tbl, COUNT(*) AS cnt FROM aggregated_notice
UNION ALL
SELECT 'aggregated_notice_public' AS tbl, COUNT(*) AS cnt FROM aggregated_notice WHERE owner_user_id IS NULL
UNION ALL
SELECT 'aggregated_notice_private' AS tbl, COUNT(*) AS cnt FROM aggregated_notice WHERE owner_user_id IS NOT NULL
UNION ALL
SELECT 'user_notice_status' AS tbl, COUNT(*) AS cnt FROM user_notice_status;

SELECT id, name, source_key, fetch_strategy, status, owner_user_id, last_fetch_status
FROM subscription_source
ORDER BY id;

SELECT id, source_id, owner_user_id, status, title
FROM aggregated_notice
ORDER BY id DESC
LIMIT 10;
