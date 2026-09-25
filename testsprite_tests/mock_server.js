const http = require('http');

let projects = [];

const server = http.createServer((req, res) => {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: 'ok' }));
        return;
    }

    const url = req.url || '/';

    if (req.method === 'GET') {
        if (url === '/' || url === '/health') {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ status: 'ok', app: 'VidoPRO', version: '2.2.0' }));
        } else if (url.startsWith('/projects') || url.startsWith('/api/projects')) {
            const sorted = [...projects].sort((a, b) => b.updatedAt - a.updatedAt);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(sorted));
        } else {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ status: 'ok', path: url }));
        }
    } else if (req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            let data = {};
            try { data = JSON.parse(body); } catch (e) {}

            const now = Date.now();
            const project = {
                id: 'proj_' + Math.random().toString(36).substring(2, 10),
                name: data.name || 'Untitled Project',
                aspectRatio: data.aspectRatio || data.aspect_ratio || '9:16',
                durationMs: 0,
                createdAt: now,
                updatedAt: now,
                tracks: []
            };
            projects.push(project);
            res.writeHead(201, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(project));
        });
    } else if (req.method === 'DELETE') {
        const parts = url.split('/').filter(Boolean);
        const pid = parts[parts.length - 1];
        projects = projects.filter(p => p.id !== pid);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, deleted: pid }));
    } else {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: 'ok' }));
    }
});

server.listen(8080, '::', () => {
    console.log('Mock VidoPRO server listening on port 8080 (dual-stack IPv4/IPv6)');
});
