let currentAlertFilter = 'all';
let currentLogFilter = 'all';

async function loadAll() {
    await Promise.all([loadStats(), loadAlerts(currentAlertFilter), loadLogs(currentLogFilter)]);
    document.getElementById('lastUpdated').textContent = 'Updated: ' + new Date().toLocaleTimeString();
}

async function loadStats() {
    try {
        const [statsRes, threatsRes] = await Promise.all([
            fetch('/svc/dashboard/stats'),
            fetch('/svc/dashboard/threats')
        ]);
        const stats = await statsRes.json();
        const threats = await threatsRes.json();

        const s = stats.data || {};
        const t = threats.data || {};

        document.getElementById('totalLogs').textContent = s.totalLogs || 0;
        document.getElementById('logsLastHour').textContent = s.logsLastHour || 0;
        document.getElementById('logsLastDay').textContent = s.logsLastDay || 0;
        document.getElementById('activeAlerts').textContent = t.activeAlerts || 0;
        document.getElementById('criticalAlerts').textContent = t.criticalAlerts || 0;
        document.getElementById('failedLogins').textContent = s.failedLoginsLastHour || 0;

        renderLogTypeChart(s.logsByType || {});
        renderThreatChart(t.alertsByType || {});
        renderTopIps(s.topIpAddresses || []);
    } catch (e) {}
}

function renderLogTypeChart(data) {
    const container = document.getElementById('logTypeChart');
    const total = Object.values(data).reduce((a, b) => a + b, 0) || 1;
    const colors = { AUTH: 'bar-auth', ACTIVITY: 'bar-activity', SYSTEM: 'bar-system' };

    if (Object.keys(data).length === 0) {
        container.innerHTML = '<p class="loading-text">No log data yet. Perform some actions.</p>';
        return;
    }

    container.innerHTML = Object.entries(data).map(([type, count]) => `
        <div class="chart-bar-row">
            <div class="chart-label">${type}</div>
            <div class="chart-bar-wrap">
                <div class="chart-bar ${colors[type] || 'bar-auth'}" style="width:${(count/total*100).toFixed(1)}%"></div>
            </div>
            <div class="chart-count">${count}</div>
        </div>
    `).join('');
}

function renderThreatChart(data) {
    const container = document.getElementById('threatTypeChart');
    const total = Object.values(data).reduce((a, b) => a + b, 0) || 1;
    const colors = {
        BRUTE_FORCE: 'bar-brute', SQL_INJECTION: 'bar-sql',
        XSS_ATTACK: 'bar-xss', PATH_TRAVERSAL: 'bar-path',
        DDOS_ATTEMPT: 'bar-ddos', ANOMALOUS_TRAFFIC: 'bar-anomal'
    };

    if (Object.keys(data).length === 0) {
        container.innerHTML = '<p class="loading-text">No threats detected yet. Try the attack simulator.</p>';
        return;
    }

    container.innerHTML = Object.entries(data).map(([type, count]) => `
        <div class="chart-bar-row">
            <div class="chart-label" style="font-size:0.72rem">${type.replace(/_/g,' ')}</div>
            <div class="chart-bar-wrap">
                <div class="chart-bar ${colors[type] || 'bar-brute'}" style="width:${(count/total*100).toFixed(1)}%"></div>
            </div>
            <div class="chart-count">${count}</div>
        </div>
    `).join('');
}

function renderTopIps(ipData) {
    const container = document.getElementById('topIpsContainer');
    if (!ipData || ipData.length === 0) {
        container.innerHTML = '<p class="loading-text">No IP data available.</p>';
        return;
    }
    const max = Math.max(...ipData.map(r => Number(r[1]))) || 1;
    container.innerHTML = ipData.slice(0, 8).map(row => {
        const ip = row[0]; const count = Number(row[1]);
        const pct = (count / max * 100).toFixed(1);
        return `<div class="ip-row">
            <span class="ip-address">${ip}</span>
            <div class="ip-bar"><div class="ip-bar-fill" style="width:${pct}%"></div></div>
            <span class="ip-count">${count} reqs</span>
        </div>`;
    }).join('');
}

async function loadAlerts(filter) {
    currentAlertFilter = filter;
    document.querySelectorAll('#alertsContainer').forEach(() => {});
    document.querySelectorAll('.card-header .filter-btn').forEach(b => {
        if (b.textContent.toLowerCase() === filter || (filter === 'all' && b.textContent === 'All')) {
            b.classList.add('active');
        } else {
            const bText = b.textContent.toLowerCase();
            if (['all','active','critical','high'].includes(bText)) b.classList.remove('active');
        }
    });

    const container = document.getElementById('alertsContainer');
    try {
        const res = await fetch('/svc/dashboard/alerts?filter=' + filter);
        const data = await res.json();
        const alerts = data.data || [];

        if (alerts.length === 0) {
            container.innerHTML = '<div class="no-alerts">✅ No threats detected. System is secure.</div>';
            return;
        }

        container.innerHTML = alerts.map(alert => `
            <div class="alert-row" id="alert-${alert.id}">
                <div>
                    <span class="severity-badge ${alert.severity?.toLowerCase()}">${alert.severity}</span>
                </div>
                <div>
                    <div class="alert-type">${alert.threatType?.replace(/_/g,' ')}</div>
                    <div class="alert-desc">${alert.description}</div>
                    <div class="alert-ip">🌐 ${alert.ipAddress} ${alert.username ? '| 👤 ' + alert.username : ''}</div>
                </div>
                <div class="alert-meta">
                    ${alert.occurrenceCount > 1 ? '<div>×' + alert.occurrenceCount + ' occurrences</div>' : ''}
                    <div>${formatTime(alert.detectedAt)}</div>
                    ${alert.resolved ? '<span class="severity-badge resolved">RESOLVED</span>' : ''}
                </div>
                <div>
                    ${!alert.resolved ? `<button class="resolve-btn" onclick="resolveAlert(${alert.id})">Resolve</button>` : ''}
                </div>
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = '<div class="no-alerts">Failed to load alerts.</div>';
    }
}

async function resolveAlert(id) {
    const btn = document.querySelector(`#alert-${id} .resolve-btn`);
    if (btn) { btn.disabled = true; btn.textContent = 'Resolving...'; }
    try {
        const res = await fetch(`/svc/dashboard/alerts/${id}/resolve`, { method: 'POST' });
        if (res.ok) {
            await loadAlerts(currentAlertFilter);
            await loadStats();
        }
    } catch (e) {}
}

async function loadLogs(type) {
    if (type) currentLogFilter = type;
    document.querySelectorAll('.admin-card:last-child .filter-btn').forEach(b => b.classList.remove('active'));

    const container = document.getElementById('logsContainer');
    try {
        const res = await fetch('/svc/dashboard/logs?type=' + currentLogFilter);
        const data = await res.json();
        const logs = data.data || [];

        if (logs.length === 0) {
            container.innerHTML = '<p class="loading-text">No logs yet.</p>';
            return;
        }

        container.innerHTML = `<table>
            <thead><tr>
                <th>ID</th><th>Type</th><th>Action</th><th>Username</th>
                <th>IP Address</th><th>Endpoint</th><th>Status</th><th>Code</th><th>Timestamp</th>
            </tr></thead>
            <tbody>${logs.map(log => `<tr>
                <td>${log.id}</td>
                <td><span class="type-badge type-${(log.logType||'').toLowerCase()}">${log.logType}</span></td>
                <td>${log.action}</td>
                <td>${log.username}</td>
                <td><code style="font-size:0.8rem">${log.ipAddress}</code></td>
                <td style="max-width:150px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:0.78rem">${log.endpoint || '—'}</td>
                <td><span class="status-${(log.status||'').toLowerCase()}">${log.status}</span></td>
                <td>${log.responseCode || '—'}</td>
                <td style="font-size:0.75rem">${formatTime(log.timestamp)}</td>
            </tr>`).join('')}</tbody>
        </table>`;
    } catch (e) {
        container.innerHTML = '<p class="loading-text">Failed to load logs.</p>';
    }
}

function formatTime(ts) {
    if (!ts) return '—';
    const d = new Date(ts);
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
}

loadAll();
setInterval(loadAll, 8000);
