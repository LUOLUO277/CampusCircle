python3 - <<'PY'
from pathlib import Path

path = Path('/etc/nginx/nginx.conf')
text = path.read_text()
needle = """        location /cy/api/actuator/ {
            proxy_pass http://127.0.0.1:18082/actuator/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

"""
insert = """        location = /cy/api/actuator/health {
            proxy_pass http://127.0.0.1:18082/actuator/health;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

"""

if insert not in text:
    text = text.replace(needle, insert + needle)
    path.write_text(text)
print("patched")
PY

nginx -t
nginx -s reload
echo reloaded
curl -i -sS http://127.0.0.1/cy/api/actuator/health
