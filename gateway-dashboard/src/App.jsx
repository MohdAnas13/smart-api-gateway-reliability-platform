import { useCallback, useEffect, useMemo, useState } from "react";
import "./App.css";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
const API_KEY = import.meta.env.VITE_API_KEY || "";
const initialMetrics = { totalRequests: 0, successfulRequests: 0, failedRequests: 0 };

function App() {
  const [metrics, setMetrics] = useState(initialMetrics);
  const [health, setHealth] = useState("Checking");
  const [userServiceHealth, setUserServiceHealth] = useState("Checking");
  const [circuitBreaker, setCircuitBreaker] = useState({
    state: "Checking",
    failureRate: -1,
    bufferedCalls: 0,
    failedCalls: 0,
    successfulCalls: 0,
  });
  const [requests, setRequests] = useState([]);
  const [lastUpdated, setLastUpdated] = useState("--:--:--");
  const [error, setError] = useState("");
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [methodFilter, setMethodFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [slowThreshold, setSlowThreshold] = useState(200);
  const [activeNav, setActiveNav] = useState("Overview");

  const fetchDashboardData = useCallback(async () => {
    setIsRefreshing(true);
    const headers = { "X-API-Key": API_KEY };
    const results = await Promise.allSettled([
      fetch(`${API_URL}/metrics`, { headers }),
      fetch(`${API_URL}/health`, { headers }),
      fetch(`${API_URL}/users`, { headers }),
      fetch(`${API_URL}/api/requests`, { headers }),
      fetch(`${API_URL}/circuit-breaker/status`, { headers }),
    ]);
    const [metricsResult, gatewayResult, userResult, historyResult, circuitResult] = results;
    const errors = [];

    if (metricsResult.status === "fulfilled" && metricsResult.value.ok) {
      try {
        const data = await metricsResult.value.json();
        setMetrics({
          totalRequests: data.totalRequests ?? 0,
          successfulRequests: data.successfulRequests ?? 0,
          failedRequests: data.failedRequests ?? 0,
        });
      } catch { errors.push("Could not read gateway metrics."); }
    } else errors.push("Could not retrieve gateway metrics.");

    if (gatewayResult.status === "fulfilled" && gatewayResult.value.ok) {
      try {
        const data = await gatewayResult.value.text();
        setHealth(data.toLowerCase().includes("running") ? "Online" : "Unavailable");
      } catch { setHealth("Unavailable"); }
    } else setHealth("Unavailable");

    setUserServiceHealth(userResult.status === "fulfilled" && userResult.value.ok ? "Online" : "Unavailable");

    if (historyResult.status === "fulfilled" && historyResult.value.ok) {
      try {
        const data = await historyResult.value.json();
        setRequests(Array.isArray(data) ? data : []);
      } catch { errors.push("Could not read request history."); }
    } else errors.push("Could not retrieve request history.");

    if (circuitResult.status === "fulfilled" && circuitResult.value.ok) {
      try {
        const data = await circuitResult.value.json();
        setCircuitBreaker({
          state: data.state ?? "UNKNOWN",
          failureRate: typeof data.failureRate === "number" ? data.failureRate : -1,
          bufferedCalls: data.bufferedCalls ?? 0,
          failedCalls: data.failedCalls ?? 0,
          successfulCalls: data.successfulCalls ?? 0,
        });
      } catch {
        setCircuitBreaker((previous) => ({ ...previous, state: "Unavailable" }));
      }
    } else {
      setCircuitBreaker((previous) => ({ ...previous, state: "Unavailable" }));
    }

    setLastUpdated(new Date().toLocaleTimeString());
    setError(errors.join(" "));
    setIsRefreshing(false);
  }, []);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      fetchDashboardData();
    }, 0);

    const intervalId = setInterval(fetchDashboardData, 5000);

    return () => {
      clearTimeout(timeoutId);
      clearInterval(intervalId);
    };
  }, [fetchDashboardData]);

  const successRate = metrics.totalRequests > 0
    ? ((metrics.successfulRequests / metrics.totalRequests) * 100).toFixed(1) : "0.0";

  const filteredRequests = useMemo(() => {
    const search = searchTerm.trim().toLowerCase();
    return requests.filter((request) => {
      const path = String(request.path ?? "").toLowerCase();
      const method = String(request.method ?? "").toUpperCase();
      const code = Number(request.statusCode);
      const matchesSearch = !search || path.includes(search);
      const matchesMethod = methodFilter === "ALL" || method === methodFilter;
      let matchesStatus = true;
      if (statusFilter === "SUCCESS") matchesStatus = code >= 200 && code < 400;
      else if (statusFilter === "CLIENT_ERROR") matchesStatus = code >= 400 && code < 500;
      else if (statusFilter === "SERVER_ERROR") matchesStatus = code >= 500 && code < 600;
      return matchesSearch && matchesMethod && matchesStatus;
    });
  }, [requests, searchTerm, methodFilter, statusFilter]);

  const performance = useMemo(() => {
    const valid = requests.filter((r) => Number.isFinite(Number(r.durationMs)) && Number(r.durationMs) >= 0);
    const durations = valid.map((r) => Number(r.durationMs));
    return durations.length ? {
      average: durations.reduce((sum, d) => sum + d, 0) / durations.length,
      fastest: Math.min(...durations),
      slowest: Math.max(...durations),
      samples: valid.slice(0, 30).reverse(),
    } : { average: 0, fastest: 0, slowest: 0, samples: [] };
  }, [requests]);

  const slowRequests = useMemo(() => requests.filter((r) => Number.isFinite(Number(r.durationMs)) && Number(r.durationMs) > slowThreshold), [requests, slowThreshold]);
  const onlineServices = [health, userServiceHealth].filter((s) => s === "Online").length;

  function resetFilters() {
    setSearchTerm(""); setMethodFilter("ALL"); setStatusFilter("ALL");
  }
  function navigateTo(section) {
    setActiveNav(section);
    const ids = { Overview: "overview", "API Metrics": "metrics-chart", Services: "services", "Request Logs": "request-logs" };
    document.getElementById(ids[section] || "overview")?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><div className="brand-icon">G</div><div className="brand-copy"><h2>Gatewatch</h2><p>API OBSERVABILITY</p></div></div>
      <div className="nav-label">WORKSPACE</div>
      <nav className="nav-list" aria-label="Main navigation">
        {[["Overview","⌂"],["API Metrics","⌁"],["Services","◈"],["Request Logs","≡"]].map(([label,icon]) =>
          <button key={label} className={`nav-item ${activeNav === label ? "active" : ""}`} onClick={() => navigateTo(label)} type="button"><span className="nav-icon">{icon}</span><span>{label}</span></button>
        )}
      </nav>
      <div className="sidebar-status"><span className="sidebar-status-icon">✦</span><div><strong>System monitoring</strong><p>Auto-refresh enabled</p></div><span className="status-pulse" /></div>
      <div className="sidebar-bottom"><div className="profile-avatar">A</div><div className="profile-copy"><strong>Administrator</strong><p>Local environment</p></div><span className="profile-menu">···</span></div>
    </aside>

    <main className="main-content" id="overview">
      <header className="topbar"><div className="page-intro"><div className="breadcrumb"><span>Workspace</span><span className="breadcrumb-slash">/</span><strong>{activeNav}</strong></div><h1>Gateway overview<span className="heading-period">.</span></h1><p className="subtitle">A clear view of your API traffic, reliability, and service health.</p></div>
        <div className="topbar-actions"><div className="environment-chip"><span className="environment-dot" />Local environment</div><button className="refresh-button" type="button" onClick={fetchDashboardData} disabled={isRefreshing}><span className={isRefreshing ? "refresh-symbol spinning" : "refresh-symbol"}>↻</span>{isRefreshing ? "Refreshing" : "Refresh"}</button></div>
      </header>
      <section className="status-strip"><div className="status-strip-main"><span className="status-strip-icon">✓</span><div><strong>Monitoring is active</strong><p>Your dashboard checks service health and request activity automatically.</p></div></div><div className="status-strip-meta"><span className="live-dot" />LIVE<span className="meta-divider" />Updated {lastUpdated}</div></section>
      {error && <div className="error-banner"><span className="error-banner-icon">!</span><div><strong>Some data could not be refreshed</strong><p>{error}</p></div></div>}

      <section className="section-heading"><div><span className="eyebrow">AT A GLANCE</span><h2>Request analytics</h2><p>Key traffic indicators from your gateway.</p></div><span className="live-badge"><span />AUTO REFRESH <b>·</b> 5 SEC</span></section>
      <section className="metrics-grid" aria-label="Request metrics">
        <MetricCard label="Total requests" value={metrics.totalRequests.toLocaleString()} description="Requests processed" icon="↗" accent="blue" />
        <MetricCard label="Successful requests" value={metrics.successfulRequests.toLocaleString()} description="HTTP responses below 400" icon="✓" accent="teal" />
        <MetricCard label="Failed requests" value={metrics.failedRequests.toLocaleString()} description="HTTP responses 400 and above" icon="!" accent="coral" />
        <MetricCard label="Success rate" value={`${successRate}%`} description="Successful requests / total" icon="%" accent="amber" />
      </section>

      <section className="overview-grid">
        <div className="panel traffic-panel"><div className="panel-header"><div><span className="panel-kicker">TRAFFIC</span><h2>Request outcomes</h2><p>Successful and failed requests recorded so far.</p></div><span className="panel-icon traffic-icon">▥</span></div>
          <div className="traffic-total"><strong>{metrics.totalRequests.toLocaleString()}</strong><span>total requests</span></div>
          <div className="traffic-bar"><div className="traffic-success" style={{width:`${metrics.totalRequests > 0 ? Math.min(100,metrics.successfulRequests/metrics.totalRequests*100):0}%`}}/><div className="traffic-failed" style={{width:`${metrics.totalRequests > 0 ? Math.min(100,metrics.failedRequests/metrics.totalRequests*100):0}%`}}/></div>
          <div className="legend"><span><i className="legend-teal"/>Successful <strong>{metrics.successfulRequests.toLocaleString()}</strong></span><span><i className="legend-coral"/>Failed <strong>{metrics.failedRequests.toLocaleString()}</strong></span></div>
          <div className="traffic-bottom-note"><span>↗</span>Success rate <strong>{successRate}%</strong></div>
        </div>
        <div className="panel services-panel" id="services"><div className="panel-header"><div><span className="panel-kicker">INFRASTRUCTURE</span><h2>Service health</h2><p>Live availability checks.</p></div><span className="panel-icon services-icon">◈</span></div>
          <div className="service-health-summary"><span className="health-summary-dot"/><strong>{onlineServices} of 2 services online</strong><span>Updated every 5 sec</span></div>
          <ServiceRow name="API Gateway" address="localhost:8080" icon="G" status={health} gateway/>
          <ServiceRow name="User Service" address="localhost:8081 · routed via gateway" icon="U" status={userServiceHealth}/>
          <div className="service-note"><span>ⓘ</span>Health status reflects the latest successful dashboard checks.</div>
        </div>
      </section>

      <section className="panel circuit-breaker-panel" aria-label="Circuit breaker status">
        <div className="panel-header">
          <div>
            <span className="panel-kicker">RELIABILITY ENGINEERING</span>
            <h2>User service circuit breaker</h2>
            <p>Protects the gateway when the user service repeatedly fails.</p>
          </div>
          <span className={`circuit-state circuit-${String(circuitBreaker.state).toLowerCase().replace(/[^a-z]/g, "-")}`}>
            <span className="circuit-state-dot" />
            {circuitBreaker.state.replace("_", " ")}
          </span>
        </div>
        <div className="circuit-breaker-stats">
          <div className="circuit-stat">
            <span>Failure rate</span>
            <strong>{circuitBreaker.failureRate < 0 ? "—" : `${circuitBreaker.failureRate.toFixed(1)}%`}</strong>
            <small>{circuitBreaker.failureRate < 0 ? "Waiting for enough calls" : "Reported by Resilience4j"}</small>
          </div>
          <div className="circuit-stat">
            <span>Calls recorded</span>
            <strong>{circuitBreaker.bufferedCalls}</strong>
            <small>In the current sliding window</small>
          </div>
          <div className="circuit-stat">
            <span>Successful calls</span>
            <strong>{circuitBreaker.successfulCalls}</strong>
            <small>Recorded by the circuit breaker</small>
          </div>
          <div className="circuit-stat">
            <span>Failed calls</span>
            <strong>{circuitBreaker.failedCalls}</strong>
            <small>Recorded by the circuit breaker</small>
          </div>
        </div>
        <div className="circuit-breaker-footer">
          <span className="footer-info-icon">i</span>
          CLOSED allows calls, OPEN blocks calls, and HALF_OPEN allows limited trial calls. Status refreshes every 5 seconds.
        </div>
      </section>

      <section className="slow-alert-panel"><div className="slow-alert-copy"><span className="slow-alert-icon">◷</span><div><span className="alert-eyebrow">PERFORMANCE WATCH</span><h2>{slowRequests.length ? "Slow requests detected" : "Response time alert"}</h2><p>{slowRequests.length ? `${slowRequests.length} recorded request${slowRequests.length === 1 ? "" : "s"} exceeded your response-time threshold.` : "No recorded requests are above your current threshold."}</p></div></div>
        <label className="slow-threshold-control">Alert threshold <span><input type="number" min="1" max="60000" value={slowThreshold} onChange={(e) => setSlowThreshold(Math.max(1,Number(e.target.value)||1))}/><strong>ms</strong></span></label>
        <div className={`slow-alert-count ${slowRequests.length ? "has-slow-requests" : "no-slow-requests"}`}><strong>{slowRequests.length}</strong><span>{slowRequests.length === 1 ? "slow request" : "slow requests"}</span></div>
      </section>

      <section className="panel performance-panel" id="metrics-chart"><div className="panel-header performance-heading"><div><span className="panel-kicker">LATENCY</span><h2>Response time performance</h2><p>Response duration for the latest requests in stored history.</p></div><span className="request-count"><span className="legend-line"/> {performance.samples.length} samples</span></div>
        <div className="performance-stats"><PerformanceCard label="Average response" value={performance.average} accent="blue" icon="◷"/><PerformanceCard label="Fastest request" value={performance.fastest} accent="teal" icon="↘"/><PerformanceCard label="Slowest request" value={performance.slowest} accent="amber" icon="↗"/></div>
        <ResponseTimeChart samples={performance.samples} slowThreshold={slowThreshold}/>
        <div className="history-footer"><span className="footer-info-icon">i</span>Values are measured in milliseconds. Chart and statistics use the currently stored request history.</div>
      </section>

      <section className="panel request-history-panel" id="request-logs"><div className="panel-header request-history-heading"><div><span className="panel-kicker">ACTIVITY</span><h2>Recent API requests</h2><p>Inspect request outcomes and response durations.</p></div><span className="request-count">{filteredRequests.length}<span>of {requests.length} records</span></span></div>
        <div className="request-filters">
          <div className="request-search"><label htmlFor="request-search">Search endpoint</label><div className="search-input-wrap"><span>⌕</span><input id="request-search" type="search" placeholder="Search endpoints…" value={searchTerm} onChange={(e)=>setSearchTerm(e.target.value)}/></div></div>
          <div className="request-filter"><label htmlFor="method-filter">HTTP method</label><select id="method-filter" value={methodFilter} onChange={(e)=>setMethodFilter(e.target.value)}><option value="ALL">All methods</option><option value="GET">GET</option><option value="POST">POST</option><option value="PUT">PUT</option><option value="DELETE">DELETE</option><option value="PATCH">PATCH</option></select></div>
          <div className="request-filter"><label htmlFor="status-filter">Status group</label><select id="status-filter" value={statusFilter} onChange={(e)=>setStatusFilter(e.target.value)}><option value="ALL">All statuses</option><option value="SUCCESS">Successful (200–399)</option><option value="CLIENT_ERROR">Client errors (400–499)</option><option value="SERVER_ERROR">Server errors (500–599)</option></select></div>
          <button type="button" className="reset-filters-button" onClick={resetFilters}>Reset filters</button>
        </div>
        <div className="request-table-wrapper"><table className="request-table"><thead><tr><th>Method</th><th>Endpoint</th><th>Status</th><th>Response time</th><th>Timestamp</th></tr></thead><tbody>
          {filteredRequests.length === 0 ? <tr><td colSpan="5" className="empty-history"><span className="empty-history-icon">⌁</span><strong>{requests.length === 0 ? "No requests recorded yet" : "No matching requests"}</strong><p>{requests.length === 0 ? "Send requests through your gateway to see activity here." : "Try changing your search or filters."}</p></td></tr> :
            filteredRequests.map((request,index) => { const duration=Number(request.durationMs); const isSlow=Number.isFinite(duration)&&duration>slowThreshold; const code=Number(request.statusCode); const statusClass=code>=500?"server":code>=400?"client":"success"; return <tr key={`${request.timestamp}-${request.method}-${request.path}-${index}`} className={isSlow?"slow-request-row":""}>
              <td><span className={`method-badge method-${String(request.method??"unknown").toLowerCase()}`}>{request.method??"—"}</span></td><td className="endpoint-cell"><code>{request.path??"—"}</code>{isSlow&&<span className="slow-row-label">SLOW</span>}</td><td><span className={`http-status http-${statusClass}`}><span/>{request.statusCode??"—"}</span></td><td><span className={`duration-value ${isSlow?"duration-slow":""}`}>{request.durationMs??"—"} ms</span></td><td className="timestamp-cell">{request.timestamp?new Date(request.timestamp).toLocaleString():"—"}</td>
            </tr>;})}
        </tbody></table></div>
        <div className="history-footer"><span>Showing {filteredRequests.length} matching requests</span><span>Latest {requests.length} stored records · refreshes every 5 seconds</span></div>
      </section>
      <footer className="app-footer"><span><span className="footer-brand-mark">G</span>Gatewatch <span className="footer-divider">·</span> API Reliability Platform</span><span>React <i/> Spring Boot</span></footer>
    </main>
  </div>;
}

function MetricCard({label,value,description,icon,accent}) {
  return <article className="metric-card"><div className="metric-card-top"><span className="metric-label">{label}</span><span className={`metric-icon ${accent}`}>{icon}</span></div><div className="metric-value">{value}</div><p className="metric-description">{description}</p><div className={`metric-card-accent ${accent}`}/></article>;
}
function PerformanceCard({label,value,accent,icon}) {
  return <article className={`performance-stat ${accent}`}><span className={`performance-stat-icon ${accent}`}>{icon}</span><span className="performance-stat-label">{label}</span><strong>{Number(value).toFixed(1)} <small>ms</small></strong></article>;
}
function ResponseTimeChart({samples,slowThreshold}) {
  const width=900,height=290,paddingLeft=62,paddingRight=24,paddingTop=22,paddingBottom=46;
  const plotWidth=width-paddingLeft-paddingRight,plotHeight=height-paddingTop-paddingBottom;
  const maximum=Math.max(10,...samples.map((s)=>Number(s.durationMs)||0));
  const points=samples.map((sample,index)=>{const duration=Number(sample.durationMs)||0;const x=samples.length===1?paddingLeft+plotWidth/2:paddingLeft+(index/(samples.length-1))*plotWidth;const y=paddingTop+plotHeight-(duration/maximum)*plotHeight;return{x,y,duration,label:sample.path??"Request",key:`${sample.timestamp}-${index}`,slow:duration>slowThreshold};});
  const linePoints=points.map((p)=>`${p.x},${p.y}`).join(" "),gridLines=[0,.25,.5,.75,1];
  return <div className="response-chart">{samples.length===0?<div className="chart-empty"><span className="chart-empty-icon">⌁</span><strong>No response-time data yet</strong><p>Send requests through your gateway to populate the chart.</p></div>:<>
    <div className="chart-legend"><span><i className="chart-legend-blue"/>Response time</span><span><i className="chart-legend-amber"/>Above threshold ({slowThreshold} ms)</span></div>
    <svg className="response-chart-svg" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Response time line chart for recent API requests">
      <defs><linearGradient id="chartAreaGradient" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#4b8ee8" stopOpacity=".18"/><stop offset="100%" stopColor="#4b8ee8" stopOpacity=".01"/></linearGradient></defs>
      {gridLines.map((f)=>{const y=paddingTop+plotHeight*(1-f);return <g key={f}><line x1={paddingLeft} y1={y} x2={width-paddingRight} y2={y} className="chart-grid-line"/><text x={paddingLeft-12} y={y+4} textAnchor="end" className="chart-axis-label">{(maximum*f).toFixed(0)}</text></g>;})}
      <line x1={paddingLeft} y1={paddingTop} x2={paddingLeft} y2={paddingTop+plotHeight} className="chart-axis-line"/><line x1={paddingLeft} y1={paddingTop+plotHeight} x2={width-paddingRight} y2={paddingTop+plotHeight} className="chart-axis-line"/>
      {points.length>1&&<polygon points={`${paddingLeft},${paddingTop+plotHeight} ${linePoints} ${width-paddingRight},${paddingTop+plotHeight}`} fill="url(#chartAreaGradient)"/>}
      {points.length>1&&<polyline points={linePoints} className="chart-line"/>}
      {points.map((p)=><g key={p.key} className="chart-point"><circle cx={p.x} cy={p.y} r={p.slow?6:4.5} className={p.slow?"chart-point-dot chart-point-slow":"chart-point-dot"}><title>{p.label}: {p.duration} ms{p.slow?" · Slow request":""}</title></circle></g>)}
      <text x={paddingLeft} y={height-13} textAnchor="start" className="chart-axis-label">Older</text><text x={width-paddingRight} y={height-13} textAnchor="end" className="chart-axis-label">Newer</text>
    </svg></>}</div>;
}
function StatusBadge({status}) {
  const cls=status==="Online"?"status-online":status==="Checking"?"status-checking":"status-offline";
  return <span className={`status-badge ${cls}`}><span/>{status}</span>;
}
function ServiceRow({name,address,icon,status,gateway=false}) {
  return <div className="service-row"><div className={`service-icon ${gateway?"gateway-icon":"user-icon"}`}>{icon}</div><div className="service-details"><strong>{name}</strong><p>{address}</p></div><StatusBadge status={status}/></div>;
}
export default App;
