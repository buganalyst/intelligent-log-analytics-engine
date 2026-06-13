async function checkSession() {
    try {
        const res = await fetch('/svc/auth/session');
        const data = await res.json();
        if (data.data && data.data.username) {
            document.getElementById('welcomeUser').textContent = '👤 ' + data.data.username;
        }
    } catch (e) {}
}

function showPanel(name) {
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.sidebar-link').forEach(l => l.classList.remove('active'));
    document.getElementById('panel-' + name).classList.add('active');
    event.target.classList.add('active');
    if (name === 'logs') loadLogs();
}

function showResult(id, message, type) {
    const box = document.getElementById(id);
    box.textContent = message;
    box.className = 'result-box ' + type;
}

async function doSearch() {
    const query = document.getElementById('searchQuery').value.trim();
    if (!query) { showResult('searchResult', '⚠ Please enter a search query', 'error'); return; }

    try {
        const res = await fetch('/svc/activity/search', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query })
        });
        const data = await res.json();
        if (data.success) {
            showResult('searchResult', '✅ ' + data.message + '\nResults: ' + data.data.resultCount + '\nTime: ' + data.data.timeTaken + '\nLog generated → Check Admin Dashboard', 'success');
        } else {
            showResult('searchResult', '❌ ' + data.message, 'error');
        }
    } catch (e) {
        showResult('searchResult', '❌ Request failed', 'error');
    }
}

async function submitForm() {
    const name = document.getElementById('formName').value.trim();
    const message = document.getElementById('formMessage').value.trim();
    if (!name) { showResult('formResult', '⚠ Please enter your name', 'error'); return; }

    try {
        const res = await fetch('/svc/activity/submit-form', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, message })
        });
        const data = await res.json();
        showResult('formResult', '✅ ' + data.message + '\nLog generated → Check Admin Dashboard', 'success');
    } catch (e) {
        showResult('formResult', '❌ Request failed', 'error');
    }
}

async function downloadFile(filename) {
    try {
        const res = await fetch('/svc/activity/download', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ filename })
        });
        const data = await res.json();
        showResult('downloadResult', '✅ ' + data.message + '\nFile: ' + filename + '\nLog generated → Check Admin Dashboard', 'success');
    } catch (e) {
        showResult('downloadResult', '❌ Request failed', 'error');
    }
}

async function simulateAttack(attackType) {
    const btn = event.currentTarget;
    btn.style.opacity = '0.6';
    btn.style.pointerEvents = 'none';

    try {
        const res = await fetch('/svc/activity/simulate-attack', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ attackType, targetUser: 'admin' })
        });
        const data = await res.json();
        showResult('simulateResult',
            '⚡ Attack Simulation: ' + attackType + '\n' +
            '✅ ' + data.message + '\n\n' +
            '→ Open Admin Dashboard to see the generated threat alert!',
            'success'
        );
    } catch (e) {
        showResult('simulateResult', '❌ Simulation failed', 'error');
    }

    setTimeout(() => {
        btn.style.opacity = '';
        btn.style.pointerEvents = '';
    }, 2000);
}

async function loadLogs() {
    const container = document.getElementById('logsTable');
    try {
        const res = await fetch('/svc/dashboard/logs');
        const data = await res.json();
        if (!data.data || data.data.length === 0) {
            container.innerHTML = '<p class="loading-text">No logs yet. Perform some actions to generate logs.</p>';
            return;
        }
        container.innerHTML = renderLogsTable(data.data);
    } catch (e) {
        container.innerHTML = '<p class="loading-text">Failed to load logs.</p>';
    }
}

function renderLogsTable(logs) {
    return `<table>
        <thead><tr>
            <th>#</th><th>Type</th><th>Action</th><th>Username</th>
            <th>IP Address</th><th>Status</th><th>Timestamp</th>
        </tr></thead>
        <tbody>${logs.map(log => `<tr>
            <td>${log.id}</td>
            <td><span class="type-badge type-${log.logType?.toLowerCase()}">${log.logType}</span></td>
            <td>${log.action}</td>
            <td>${log.username}</td>
            <td><code>${log.ipAddress}</code></td>
            <td><span class="status-${log.status?.toLowerCase()}">${log.status}</span></td>
            <td>${formatTime(log.timestamp)}</td>
        </tr>`).join('')}</tbody>
    </table>`;
}

async function logout() {
    await fetch('/svc/auth/logout', { method: 'POST' });
    window.location.href = '/';
}

function formatTime(ts) {
    if (!ts) return '—';
    return new Date(ts).toLocaleString();
}

checkSession();

fetch('/svc/activity/page-visit?page=/dashboard').catch(() => {});
