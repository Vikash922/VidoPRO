import json
import time
import uuid
from http.server import HTTPServer, BaseHTTPRequestHandler

projects = []

class VidoProHandler(BaseHTTPRequestHandler):
    def _send_json(self, status_code, data):
        self.send_response(status_code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.end_headers()
        self.wfile.write(json.dumps(data).encode('utf-8'))

    def do_OPTIONS(self):
        self._send_json(200, {"status": "ok"})

    def do_GET(self):
        if self.path == '/' or self.path == '/health':
            self._send_json(200, {"status": "ok", "app": "VidoPRO", "version": "2.2.0"})
        elif self.path.startswith('/projects') or self.path.startswith('/api/projects'):
            # Return list of projects ordered by updatedAt desc
            sorted_projects = sorted(projects, key=lambda p: p.get('updatedAt', 0), reverse=True)
            self._send_json(200, sorted_projects)
        else:
            self._send_json(200, {"status": "ok", "path": self.path})

    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body = {}
        if content_length > 0:
            try:
                body = json.loads(self.rfile.read(content_length).decode('utf-8'))
            except Exception:
                pass

        now = int(time.time() * 1000)
        project_id = str(uuid.uuid4())
        name = body.get('name') or "Untitled Project"
        aspect_ratio = body.get('aspectRatio') or body.get('aspect_ratio') or "9:16"

        new_project = {
            "id": project_id,
            "name": name,
            "aspectRatio": aspect_ratio,
            "durationMs": 0,
            "createdAt": now,
            "updatedAt": now,
            "tracks": []
        }
        projects.append(new_project)
        self._send_json(201, new_project)

    def do_DELETE(self):
        parts = self.path.strip('/').split('/')
        if len(parts) >= 2 and (parts[0] in ('projects', 'api')):
            pid = parts[-1]
            global projects
            projects = [p for p in projects if p.get('id') != pid]
            self._send_json(200, {"success": True, "deleted": pid})
        else:
            self._send_json(200, {"success": True})

def run(port=8080):
    server = HTTPServer(('127.0.0.1', port), VidoProHandler)
    print(f"Mock VidoPRO server running on http://127.0.0.1:{port}")
    server.serve_forever()

if __name__ == '__main__':
    run()
