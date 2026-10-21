(() => {
  'use strict';

  const I = (name) => window.GEDIcons.svg(name);
  const $ = (sel, root=document) => root.querySelector(sel);
  const $$ = (sel, root=document) => [...root.querySelectorAll(sel)];
  const storage = {
    get(key, fallback){ try{ const v=localStorage.getItem(key); return v===null?fallback:JSON.parse(v); }catch{return fallback;} },
    set(key, value){ try{ localStorage.setItem(key, JSON.stringify(value)); }catch{} }
  };

  const state = {
    page: 'dashboard',
    online: false,
    busy: false,
    status: null,
    settings: {
      root: 'documentos', inbox:'AAA', review:'AAA\\REVISAO', errors:'AAA\\ERROS', threshold:95,
      destination_category:'RELATÓRIOS', create_folders:true, hash_duplicates:true, simulation:false, auto_process:true
    },
    queue: [],
    results: [],
    rules: [],
    companies: [],
    history: [],
    queueError: null,
    companyRegistration: {file:null, base64:null, preview:null, busy:false},
  };

  const pageNames = {dashboard:'Visão geral',inbox:'Caixa de entrada',review:'Revisão',companies:'Empresas',rules:'Regras',history:'Histórico',settings:'Configurações'};

  function esc(s=''){ return String(s ?? '').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
  function initials(name=''){ return name.split(/\s+/).filter(Boolean).slice(0,2).map(x=>x[0]).join('').toUpperCase(); }
  function fmtBytes(n=0){ return n<1024?`${n} B`:n<1024*1024?`${Math.round(n/1024)} KB`:`${(n/1024/1024).toFixed(1)} MB`; }
  function formatTime(ts){ if(!ts)return '—'; const d=new Date(ts*1000); return d.toLocaleString('pt-BR',{day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'}); }
  function apiUrl(path){ return `/api/${path}`; }

  async function api(path, options={}){
    const res = await fetch(apiUrl(path), {cache:'no-store', headers:{'Content-Type':'application/json', ...(options.headers||{})}, ...options});
    let body={}; try{ body=await res.json(); }catch{}
    if(!res.ok) throw new Error(body.error || `HTTP ${res.status}`);
    return body;
  }

  function toast(title, message='', icon='circle-info'){
    const el=document.createElement('div'); el.className='toast';
    el.innerHTML=`${I(icon)}<div class="toast__text"><strong>${esc(title)}</strong><small>${esc(message)}</small></div>`;
    $('#toastRoot').appendChild(el); setTimeout(()=>el.remove(),3500);
  }

  function statusBadge(status){
    const map={waiting:['Aguardando','waiting'],unconfigured:['Não parametrizado','warning'],recognized:['Arquivado','success'],review:['Revisão','warning'],error:['Erro','error']};
    const [label,tone]=map[status]||map.waiting; return `<span class="badge badge--${tone}">${label}</span>`;
  }
  function stat(label, value, hint, icon, tone){ return `<div class="stat" data-tone="${tone}"><div class="stat__label">${esc(label)}</div><div class="stat__value">${esc(value)}</div><div class="stat__hint">${esc(hint)}</div><div class="stat__icon">${I(icon)}</div></div>`; }
  function resultByName(name){ return state.results.find(r=>r.name===name); }
  function counts(){
    const c={recognized:0,review:0,unconfigured:0,error:0};
    state.results.forEach(r=>{c[r.status]=(c[r.status]||0)+1;}); return c;
  }

  function updateChrome(){
    const online=state.online;
    $('#serverLabel').textContent = online ? (state.status?.root_exists ? 'Servidor Exemplo' : 'Motor online • raiz ausente') : 'Motor offline';
    $('#serverChip').classList.toggle('is-offline',!online || !state.status?.root_exists);
    $('#serverStatusDot').classList.toggle('is-offline',!online || !state.status?.root_exists);
    $('#navInboxBadge').textContent=state.queue.length;
    $('#navReviewBadge').textContent=state.results.filter(r=>r.status==='review'||r.archive_action==='review').length;
    $('#navCompaniesCount').textContent=state.companies.length || 24;
    $('#navRulesCount').textContent=state.rules.length || 24;
  }

  function engineOfflineNotice(){
    if(state.online) return '';
    return `<div class="notice notice--danger">${I('triangle-alert')}<div><strong>Motor local não conectado.</strong><br>Abra o projeto pelo <code>run.bat</code>. Abrir apenas o <code>index.html</code> não dá acesso à pasta AAA nem ao analisador de PDFs.</div></div>`;
  }

  function rootNotice(){
    if(!state.online || state.status?.root_exists) return '';
    return `<div class="notice">${I('circle-info')}<div><strong>O motor está online, mas a pasta raiz ainda não existe neste computador.</strong><br>Raiz configurada: <span class="mono">${esc(state.settings.root)}</span></div></div>`;
  }

  function dashboard(){
    const c=counts();
    const waiting = state.results.length ? Math.max(0,state.queue.length-state.results.length) : state.queue.length;
    return `<div class="page">
      <div class="page-header"><div class="page-title"><h1>Visão geral</h1><p>Monitoramento automático da pasta AAA do Servidor Exemplo.</p></div><div class="page-actions"><span class="badge badge--${state.status?.monitor_running?'success':'warning'}">${I(state.status?.monitor_running?'activity':'triangle-alert')} ${state.status?.monitor_running?'MONITOR ATIVO':'MONITOR INATIVO'}</span><button class="btn btn--secondary" data-go="settings">${I('sliders')} Configurar</button></div></div>
      ${engineOfflineNotice()}${rootNotice()}
      <div class="stats-grid">
        ${stat('Arquivos em AAA',state.queue.length,'Documentos encontrados fisicamente','inbox','blue')}
        ${stat('Arquivados recentemente',c.recognized,'Movidos automaticamente para RELATÓRIOS','circle-check','green')}
        ${stat('Para revisão',c.review,'Exigem validação humana','file-search','orange')}
        ${stat('Erros / não parametrizados',c.error+c.unconfigured,'Sem reconhecimento seguro','triangle-alert','red')}
      </div>
      <div class="grid-main">
        <div class="panel"><div class="panel__header"><div><div class="panel__title">Pipeline ativo</div><div class="panel__subtitle">A v0.0.6 monitora a AAA e arquiva relatórios de empresas cadastradas automaticamente</div></div><span class="badge badge--blue">v0.0.6 • Auto</span></div><div class="panel__body"><div class="pipeline">
          <div class="pipeline__step"><span class="pipeline__num">01</span><strong>Ler PDF</strong><small>Extração da camada de texto pelo motor Java local.</small></div>
          <div class="pipeline__step"><span class="pipeline__num">02</span><strong>Reconhecer</strong><small>${state.rules.length||12} regras documentais parametrizadas.</small></div>
          <div class="pipeline__step"><span class="pipeline__num">03</span><strong>Identificar</strong><small>CNPJ, empresa e competência.</small></div>
          <div class="pipeline__step"><span class="pipeline__num">04</span><strong>Renomear</strong><small>Gera o nome padronizado proposto.</small></div>
          <div class="pipeline__step"><span class="pipeline__num">05</span><strong>Realocar</strong><small>Cria a estrutura ausente e move o arquivo automaticamente.</small></div>
        </div></div></div>
        <div class="panel"><div class="panel__header"><div><div class="panel__title">Servidor monitorado</div><div class="panel__subtitle">Configuração atual do motor</div></div></div><div class="panel__body">
          <div class="folder-card"><div class="folder-card__icon">${I('folder-input')}</div><div class="folder-card__info"><strong>${esc(state.settings.inbox||'AAA')}</strong><small>${esc(state.settings.root)}\\${esc(state.settings.inbox||'AAA')}</small></div><div class="folder-card__count">${state.queue.length}</div></div>
          <div class="engine-meta"><div><span>Motor</span><strong>${state.online?'Online':'Offline'}</strong></div><div><span>Limiar</span><strong>${esc(state.settings.threshold)}%</strong></div><div><span>Destino</span><strong>${esc(state.settings.destination_category||'RELATÓRIOS')}</strong></div><div><span>Arquivos</span><strong>Automático</strong></div></div>
        </div></div>
      </div>
      <div class="grid-main">
        <div class="panel"><div class="panel__header"><div><div class="panel__title">Documentos recentes</div><div class="panel__subtitle">Resultado mais recente do reconhecimento</div></div><button class="btn btn--ghost" data-go="inbox">Abrir caixa ${I('arrow-right')}</button></div><div class="panel__body panel__body--flush"><div class="table-wrap"><table class="table"><thead><tr><th>Arquivo</th><th>Status</th><th>Documento</th><th>Empresa</th><th>Competência</th></tr></thead><tbody>${resultRows(state.results.slice(0,6),false)}</tbody></table></div></div></div>
        <div class="panel"><div class="panel__header"><div><div class="panel__title">Escopo parametrizado</div><div class="panel__subtitle">Empresas já vinculadas por CNPJ</div></div></div><div class="panel__body"><div class="quick-list">
          ${state.companies.filter(c=>c.parametrized).slice(0,4).map(c=>`<div class="quick-action static"><span class="quick-action__icon">${I('building-2')}</span><span class="quick-action__text"><strong>${esc(c.folder)}</strong><small>${esc(c.cnpj||'CNPJ pendente')}</small></span>${I('check')}</div>`).join('') || `<div class="empty compact"><p>Nenhuma empresa parametrizada carregada.</p></div>`}
        </div></div></div>
      </div>
    </div>`;
  }

  function fileRows(items, detailed=true){
    if(!items.length) return `<tr><td colspan="${detailed?7:5}"><div class="empty"><div class="empty__icon">${I('inbox')}</div><h3>AAA está vazia</h3><p>A pasta está livre. Basta jogar PDFs de relatórios na AAA; o monitor os processa automaticamente.</p></div></td></tr>`;
    return items.map(f=>{
      const r=resultByName(f.name); const status=r?.status||'waiting';
      const title=r?.document?.name||'—'; const company=r?.company?.folder||'—'; const competence=r?.competence?.value||'—';
      return `<tr class="${r?'is-clickable':''}" data-result-name="${esc(f.name)}">
        <td><div class="file-cell"><div class="file-icon">${I('file-text')}</div><div class="file-cell__text"><strong>${esc(f.name)}</strong><small>${fmtBytes(f.size)} • ${formatTime(f.mtime)}</small></div></div></td>
        <td>${statusBadge(status)}</td><td class="cell-ellipsis">${esc(title)}</td><td class="cell-ellipsis">${esc(company)}</td><td>${esc(competence)}</td>
        ${detailed?`<td>${r?`<span class="confidence confidence--${r.confidence>=95?'high':r.confidence>=70?'mid':'low'}">${esc(r.confidence)}%</span>`:'—'}</td><td>${r?`<button class="icon-button js-view" title="Detalhes">${I('eye')}</button>`:''}</td>`:''}
      </tr>`;
    }).join('');
  }

  function resultRows(items, detailed=true){
    if(!items.length) return `<tr><td colspan="${detailed?7:5}"><div class="empty"><div class="empty__icon">${I('history')}</div><h3>Nenhuma atividade recente</h3><p>Os documentos processados automaticamente aparecerão aqui.</p></div></td></tr>`;
    return items.map(r=>{
      const title=r?.document?.name||'—', company=r?.company?.folder||'—', competence=r?.competence?.value||'—';
      const finalPath=r.final_path||r.destination||'—';
      return `<tr class="is-clickable" data-result-name="${esc(r.name)}">
        <td><div class="file-cell"><div class="file-icon">${I(r.archive_action==='archived'?'file-check-2':r.archive_action==='error'?'triangle-alert':'file-search')}</div><div class="file-cell__text"><strong>${esc(r.final_name||r.name)}</strong><small>${esc(r.archive_action==='archived'?'Arquivado automaticamente':r.archive_action==='error'?'Enviado para ERROS':'Enviado para REVISÃO')}</small></div></div></td>
        <td>${statusBadge(r.status)}</td><td class="cell-ellipsis">${esc(title)}</td><td class="cell-ellipsis">${esc(company)}</td><td>${esc(competence)}</td>
        ${detailed?`<td><span class="confidence confidence--${r.confidence>=95?'high':r.confidence>=70?'mid':'low'}">${esc(r.confidence||0)}%</span></td><td><button class="icon-button js-view" title="Detalhes">${I('eye')}</button></td>`:''}
      </tr>`;
    }).join('');
  }

  function inbox(){
    return `<div class="page">
      <div class="page-header"><div class="page-title"><h1>Caixa de entrada</h1><p>Arquivos lidos diretamente de <strong>${esc(state.settings.root)}\\${esc(state.settings.inbox)}</strong>.</p></div><div class="page-actions"><span class="badge badge--${state.status?.monitor_running?'success':'warning'}">${I(state.status?.monitor_running?'activity':'triangle-alert')} ${state.status?.monitor_running?'Monitorando AAA':'Monitor inativo'}</span><button class="btn btn--secondary" id="scanInbox" ${!state.online||state.busy?'disabled':''}>${I('refresh-cw')} Atualizar tela</button></div></div>
      ${engineOfflineNotice()}${rootNotice()}
      <div class="inbox-banner"><div class="inbox-banner__icon">${I('folder-input')}</div><div><strong>Fluxo automático da v0.0.6</strong><p>Jogue um PDF de relatório diretamente na AAA. Assim que a cópia terminar, o GED reconhece documento, empresa e competência, cria a estrutura necessária, renomeia e move o arquivo para RELATÓRIOS. Itens inseguros são segregados em REVISÃO e falhas técnicas em ERROS.</p></div><span class="badge badge--success">AUTOMÁTICO</span></div>
      ${state.status?.monitor_error?`<div class="notice notice--danger">${I('triangle-alert')}<div><strong>Monitor da AAA:</strong><br>${esc(state.status.monitor_error)}</div></div>`:''}
      ${state.queueError?`<div class="notice notice--danger">${I('triangle-alert')}<div><strong>Não foi possível ler AAA.</strong><br>${esc(state.queueError)}</div></div>`:''}
      <div class="panel"><div class="panel__header"><div><div class="panel__title">Arquivos aguardando na AAA</div><div class="panel__subtitle">Normalmente esta fila fica vazia porque o processamento é automático</div></div><span class="table-count">${state.queue.length} arquivo(s)</span></div><div class="panel__body panel__body--flush"><div class="table-wrap"><table class="table"><thead><tr><th style="width:29%">Arquivo</th><th>Status</th><th>Documento</th><th>Empresa</th><th>Competência</th><th>Confiança</th><th style="width:54px"></th></tr></thead><tbody>${fileRows(state.queue,true)}</tbody></table></div></div></div>
      <div class="panel"><div class="panel__header"><div><div class="panel__title">Processamento recente</div><div class="panel__subtitle">Últimos documentos tratados pelo monitor automático</div></div></div><div class="panel__body panel__body--flush"><div class="table-wrap"><table class="table"><thead><tr><th style="width:29%">Arquivo</th><th>Status</th><th>Documento</th><th>Empresa</th><th>Competência</th><th>Confiança</th><th style="width:54px"></th></tr></thead><tbody>${resultRows(state.results,true)}</tbody></table></div></div></div>
    </div>`;
  }

  function review(){
    const reviewItems=state.results.filter(r=>r.archive_action==='review'||r.status==='review'||r.status==='unconfigured');
    return `<div class="page"><div class="page-header"><div class="page-title"><h1>Revisão</h1><p>Documentos que o monitor retirou da AAA, mas não pôde arquivar com segurança.</p></div></div>
      <div class="notice">${I('circle-info')}<div><strong>Regra de segurança:</strong> documentos abaixo de ${esc(state.settings.threshold)}% de confiança, não parametrizados ou com colisão de nome são movidos para <code>${esc(state.settings.review||'AAA\\REVISAO')}</code>. Eles nunca são enviados automaticamente para a pasta da empresa.</div></div>
      <div class="panel"><div class="panel__body panel__body--flush">${reviewItems.length?`<div class="review-list">${reviewItems.map(reviewCard).join('')}</div>`:`<div class="empty"><div class="empty__icon">${I('circle-check')}</div><h3>Nenhuma revisão recente</h3><p>O monitor continuará acompanhando a AAA em segundo plano enquanto o GED estiver aberto.</p></div>`}</div></div>
    </div>`;
  }

  function reviewCard(r){
    const detected=(r.company?.detected_cnpjs||[]).join(', ');
    return `<button class="review-card js-result-card" data-result-name="${esc(r.name)}"><div class="review-card__icon">${I('file-search')}</div><div class="review-card__main"><strong>${esc(r.name)}</strong><small>${esc(r.document?.name||'Documento não reconhecido')} • ${esc(r.company?.folder||detected||'Empresa não identificada')} • ${esc(r.competence?.value||'Competência ausente')}</small>${r.review_reason?`<span>${esc(r.review_reason)}</span>`:(r.notice?`<span>${esc(r.notice)}</span>`:'')}</div><div class="review-card__score"><strong>${esc(r.confidence||0)}%</strong><small>confiança</small></div>${I('chevron-right')}</button>`;
  }

  function companiesPage(){
    return `<div class="page"><div class="page-header"><div class="page-title"><h1>Empresas</h1><p>Cadastro mestre por Cartão CNPJ. Somente empresas cadastradas aqui podem receber documentos automaticamente.</p></div><div class="page-actions"><button class="btn btn--secondary" id="reloadCompanies" ${!state.online?'disabled':''}>${I('refresh-cw')} Atualizar</button><button class="btn btn--primary" id="registerCompany" ${!state.online||!state.status?.root_exists?'disabled':''}>${I('building-2')} Cadastrar por Cartão CNPJ</button></div></div>
      <div class="notice">${I('circle-info')}<div><strong>Cartão CNPJ é a porta de entrada da empresa.</strong> O GED extrai CNPJ e razão social, registra a empresa e cria sua pasta na raiz. Pastas soltas no servidor não são consideradas empresas cadastradas.</div></div>
      <div class="toolbar"><div class="searchbox">${I('search')}<input id="companySearch" placeholder="Pesquisar empresa ou CNPJ..." /></div><div class="toolbar__spacer"></div><span class="table-count"><strong>${state.companies.length}</strong> empresa(s) cadastrada(s)</span></div>
      <div class="panel"><div class="panel__body panel__body--flush"><div class="table-wrap"><table class="table"><thead><tr><th>Empresa / pasta</th><th>CNPJ</th><th>Vínculo</th><th>Estrutura</th></tr></thead><tbody id="companyRows">${companyRows(state.companies)}</tbody></table></div></div></div>
    </div>`;
  }
  function companyRows(list){
    if(!list.length)return `<tr><td colspan="4"><div class="empty"><p>Nenhuma empresa cadastrada. Use “Cadastrar por Cartão CNPJ” para criar a primeira.</p></div></td></tr>`;
    return list.map(c=>`<tr><td><div class="company-name"><span class="company-avatar">${esc(initials(c.folder))}</span><strong>${esc(c.folder)}</strong></div></td><td class="mono ${c.cnpj?'':'muted'}">${esc(c.cnpj||'Pendente')}</td><td>${c.parametrized?'<span class="badge badge--success">CNPJ ativo</span>':'<span class="badge badge--waiting">Cadastro incompleto</span>'}</td><td>${c.exists?'<span class="badge badge--success">Encontrada</span>':'<span class="badge badge--waiting">Configuração</span>'}</td></tr>`).join('');
  }

  function rulesPage(){
    return `<div class="page"><div class="page-header"><div class="page-title"><h1>Regras e reconhecedores</h1><p>Parametrizações ativas utilizadas pelo motor documental da v0.0.6.</p></div></div>
      <div class="notice">${I('circle-info')}<div><strong>${state.rules.length} reconhecedores ativos.</strong> Nesta etapa todos os documentos parametrizados são propostos para a subpasta <strong>RELATÓRIOS</strong>. As regras ainda são editadas nos arquivos de configuração do projeto.</div></div>
      <div class="panel"><div class="panel__header"><div><div class="panel__title">Catálogo documental</div><div class="panel__subtitle">Assinaturas textuais, nome de saída e prioridade</div></div></div><div class="panel__body panel__body--flush">${state.rules.length?state.rules.map(ruleCard).join(''):`<div class="empty"><p>Motor não conectado ou regras não carregadas.</p></div>`}</div></div>
    </div>`;
  }
  function ruleCard(r){
    return `<div class="rule-card rule-card--engine"><div class="rule-card__icon">${I(r.fallback?'file-search':'workflow')}</div><div class="rule-card__main"><strong>${esc(r.name)}</strong><small>${(r.signatures||[]).map(s=>`<code>${esc(s)}</code>`).join(' + ')}</small><span class="rule-path">{empresa}\\{ano}\\{competencia}\\${esc(r.category||'RELATÓRIOS')}</span></div><div class="rule-card__meta"><strong>${esc(r.filename)}</strong><small>${r.fallback?'Fallback por metadados/nome':'Leitura por conteúdo'} • prioridade ${esc(r.priority)}</small></div></div>`;
  }

  function historyPage(){
    return `<div class="page"><div class="page-header"><div class="page-title"><h1>Histórico</h1><p>Trilha local de arquivamentos automáticos, revisões, erros e alterações de configuração.</p></div><div class="page-actions"><button class="btn btn--secondary" id="reloadHistory" ${!state.online?'disabled':''}>${I('refresh-cw')} Atualizar</button></div></div>
      <div class="panel"><div class="panel__header"><div><div class="panel__title">Atividades</div><div class="panel__subtitle">Arquivo local data/history.jsonl</div></div></div><div class="panel__body"><div class="timeline">${state.history.length?state.history.map(historyItem).join(''):`<div class="empty"><div class="empty__icon">${I('history')}</div><h3>Histórico vazio</h3><p>O primeiro arquivo colocado na AAA criará um registro automaticamente.</p></div>`}</div></div></div>
    </div>`;
  }
  function historyItem(h){
    const time=h.timestamp?new Date(h.timestamp).toLocaleString('pt-BR'):'—';
    const detail=h.counts?`Reconhecidos ${h.counts.recognized||0} • Revisão ${h.counts.review||0} • Não parametrizados ${h.counts.unconfigured||0} • Erros ${h.counts.error||0}`:'';
    const label=h.type==='auto_archive'?'Arquivamento automático':h.type==='auto_review'?'Encaminhado para revisão':h.type==='auto_error'?'Erro automático':h.type==='process'?'Varredura manual':h.type==='company_registration'?'Cadastro de empresa':'Configuração';
    return `<div class="timeline__item"><span class="timeline__dot"></span><div class="timeline__text"><strong>${esc(label)}</strong><small>${esc(h.message||'Evento registrado')}${detail?` • ${esc(detail)}`:''}${h.created_dirs?.length?` • ${esc(h.created_dirs.length)} pasta(s) criada(s)`:''}</small></div><span class="timeline__time">${esc(time)}</span></div>`;
  }

  function settingsPage(){
    const s=state.settings;
    return `<div class="page"><div class="page-header"><div class="page-title"><h1>Configurações</h1><p>Parâmetros consumidos diretamente pelo motor local.</p></div></div>${engineOfflineNotice()}
      <div class="settings-grid"><div class="panel"><div class="panel__header"><div><div class="panel__title">Servidor e processamento</div><div class="panel__subtitle">Salvo em config/settings.json</div></div></div><div class="panel__body"><form id="settingsForm" class="form">
        <div class="field"><label>Pasta raiz</label><input class="input" name="root" value="${esc(s.root)}" /><small>Informe a pasta raiz dos documentos (padrão: documentos).</small></div>
        <div class="field-row"><div class="field"><label>Pasta de entrada</label><input class="input" name="inbox" value="${esc(s.inbox||'AAA')}" /></div><div class="field"><label>Confiança mínima (%)</label><input class="input" name="threshold" type="number" min="1" max="100" value="${esc(s.threshold||95)}" /></div></div>
        <div class="field-row"><div class="field"><label>Pasta lógica de revisão</label><input class="input" name="review" value="${esc(s.review||'AAA\\REVISAO')}" /></div><div class="field"><label>Subpasta de destino</label><input class="input" name="destination_category" value="RELATÓRIOS" disabled /><small>Escopo fixo da v0.0.6: somente PDFs classificados como RELATÓRIOS. XML, notas fiscais em PDF e arquivos SPED ainda não entram no fluxo automático.</small></div></div>
        <div>
          <div class="setting-line"><div class="setting-line__text"><strong>Monitoramento automático da AAA</strong><small>Quando ativo, nenhum botão de processamento é necessário: arquivos novos são tratados assim que terminam de ser copiados.</small></div><label class="switch"><input name="auto_process" type="checkbox" ${s.auto_process!==false?'checked':''}><span></span></label></div>
          <div class="setting-line"><div class="setting-line__text"><strong>Criar pastas ausentes</strong><small>Ativo: o GED pode criar empresa, ano, competência e RELATÓRIOS quando a estrutura ainda não existir.</small></div><label class="switch"><input name="create_folders" type="checkbox" ${s.create_folders?'checked':''}><span></span></label></div>
          <div class="setting-line"><div class="setting-line__text"><strong>Detectar duplicados por SHA-256</strong><small>O motor já calcula hash durante o processamento e marca duplicados presentes na própria AAA.</small></div><label class="switch"><input name="hash_duplicates" type="checkbox" ${s.hash_duplicates?'checked':''}><span></span></label></div>
        </div>
        <div><button class="btn btn--primary" type="submit" ${!state.online?'disabled':''}>${I('save')} Salvar configurações</button></div>
      </form></div></div>
      <div class="panel"><div class="about-card"><div class="about-logo">${I('database')}</div><h3>GED</h3><p>Gestão Eletrônica de Documentos • motor local</p><div class="about-meta"><div><span>Versão</span><strong>0.0.6</strong></div><div><span>Engine</span><strong>Java + Apache PDFBox</strong></div><div><span>Entrada</span><strong>${esc(s.inbox||'AAA')}</strong></div><div><span>Arquivos</span><strong>Automático</strong></div><div><span>Regras</span><strong>${state.rules.length}</strong></div><div><span>Admin</span><strong>Não exige</strong></div></div></div></div></div>
    </div>`;
  }

  function render(){
    const fn={dashboard,inbox,review,companies:companiesPage,rules:rulesPage,history:historyPage,settings:settingsPage}[state.page];
    $('#pageContent').innerHTML=fn();
    $('#breadcrumbCurrent').textContent=pageNames[state.page];
    $$('.nav__item').forEach(b=>b.classList.toggle('is-active',b.dataset.page===state.page));
    window.GEDIcons.render($('#pageContent')); bindPage(); updateChrome();
  }
  function go(page){ if(!pageNames[page])return; state.page=page; render(); }

  async function loadStatus(){
    try{
      const data=await api('status'); state.online=true; state.status=data; state.settings={...state.settings,...data.settings};
    }catch(e){ state.online=false; state.status=null; }
  }
  async function loadQueue(){
    if(!state.online){state.queue=[];return;}
    try{ const data=await api('queue'); state.queue=data.items||[]; state.queueError=data.error||null; }
    catch(e){state.queue=[];state.queueError=e.message;}
  }
  async function loadRules(){ if(!state.online)return; try{state.rules=(await api('rules')).items||[];}catch{} }
  async function loadCompanies(){ if(!state.online)return; try{state.companies=(await api('companies')).items||[];}catch{} }
  async function loadHistory(){ if(!state.online)return; try{state.history=(await api('history')).items||[];}catch{} }
  async function loadRecent(){ if(!state.online)return; try{state.results=(await api('recent')).items||[];}catch{} }
  async function refreshAll(showToast=false){
    await loadStatus();
    if(state.online) await Promise.all([loadQueue(),loadRules(),loadCompanies(),loadHistory(),loadRecent()]);
    render(); if(showToast) toast(state.online?'Dados atualizados':'Motor não conectado',state.online?'AAA e configurações foram relidas.':'Execute run.bat para iniciar o motor.',state.online?'refresh-cw':'triangle-alert');
  }

  async function processAll(){
    if(!state.online){toast('Motor offline','Execute run.bat antes de processar.','triangle-alert');return;}
    state.busy=true; render();
    try{
      const data=await api('process',{method:'POST',body:'{}'}); state.results=data.items||[];
      await Promise.all([loadQueue(),loadHistory()]);
      const c=data.counts||{};
      toast('Análise concluída',`${state.results.length} arquivo(s): ${c.recognized||0} reconhecido(s), ${c.review||0} em revisão • ${(data.created_dirs||[]).length} pasta(s) criada(s).`, 'circle-check');
    }catch(e){ toast('Falha no processamento',e.message,'triangle-alert'); }
    finally{state.busy=false;render();}
  }

  function fileToBase64(file){
    return new Promise((resolve,reject)=>{ const reader=new FileReader(); reader.onload=()=>resolve(String(reader.result).split(',')[1]||''); reader.onerror=()=>reject(new Error('Não foi possível ler o PDF selecionado.')); reader.readAsDataURL(file); });
  }

  function openCompanyRegistration(){
    state.companyRegistration={file:null,base64:null,preview:null,busy:false};
    renderCompanyRegistrationModal();
  }

  function renderCompanyRegistrationModal(){
    const cr=state.companyRegistration; const p=cr.preview; const card=p?.card||{};
    $('#modalRoot').innerHTML=`<div class="overlay" id="companyModalOverlay"><div class="modal modal--wide"><div class="modal__header"><div><h2>Cadastrar empresa por Cartão CNPJ</h2><p>Leitura automática do Comprovante de Inscrição e de Situação Cadastral</p></div><button class="close-button" id="closeCompanyModal">${I('x')}</button></div><div class="modal__body">
      <div class="registration-dropzone ${cr.file?'has-file':''}" id="companyCardDropzone" tabindex="0">
        <div class="registration-dropzone__icon">${I(cr.file?'file-text':'upload')}</div><div><strong>${esc(cr.file?.name||'Selecione ou arraste o Cartão CNPJ em PDF')}</strong><small>${cr.file?`${fmtBytes(cr.file.size)} • pronto para leitura`:'PDF oficial emitido pela Receita Federal • até 8 MB'}</small></div><button class="btn btn--secondary btn--sm" id="pickCompanyCard" type="button">${I('folder-open')} Escolher PDF</button><input id="companyCardInput" type="file" accept="application/pdf,.pdf" hidden />
      </div>
      ${p?`${card.valid?'':`<div class="notice notice--danger">${I('triangle-alert')}<div><strong>O cartão não passou nas validações.</strong><br>${esc((card.errors||[]).join(' '))}</div></div>`}
        <div class="company-preview-head"><div><span class="confidence confidence--${card.confidence>=95?'high':card.confidence>=70?'mid':'low'}">${esc(card.confidence||0)}%</span><strong>${esc(card.legal_name||'Razão social não identificada')}</strong><small>${esc(card.cnpj||'CNPJ não identificado')}${card.trade_name?` • ${esc(card.trade_name)}`:''}</small></div>${card.status?`<span class="badge ${card.status==='ATIVA'?'badge--success':'badge--warning'}">${esc(card.status)}</span>`:''}</div>
        <div class="diagnostic-grid company-card-grid">
          <div class="diagnostic"><span>Razão social</span><strong>${esc(card.legal_name||'—')}</strong><small>Pasta proposta: ${esc(card.folder||'—')}</small></div>
          <div class="diagnostic"><span>CNPJ</span><strong>${esc(card.cnpj||'—')}</strong><small>Identificador mestre da empresa</small></div>
          <div class="diagnostic"><span>Nome fantasia</span><strong>${esc(card.trade_name||'—')}</strong><small>${esc(card.main_activity||'Atividade principal não lida')}</small></div>
          <div class="diagnostic"><span>Localização</span><strong>${esc([card.municipality,card.uf].filter(Boolean).join(' / ')||'—')}</strong><small>Extraído do cartão</small></div>
        </div>
        <div class="result-section"><span>Pasta da empresa</span><code>${esc(p.folder_path||'—')}</code></div>
        ${p.existing_company?`<div class="notice">${I('circle-info')}<div><strong>Este CNPJ já está cadastrado.</strong> Confirmar apenas garante que a pasta vinculada exista; o GED não cria uma duplicata.</div></div>`:`<div class="notice notice--success">${I('circle-check')}<div><strong>Novo cadastro.</strong> Ao confirmar, o vínculo será salvo em <code>config/companies.json</code> e a pasta será criada na raiz se necessário.</div></div>`}
      `:''}
    </div><div class="modal__footer"><span class="modal-note">A raiz do Servidor Exemplo nunca é criada automaticamente; ela precisa existir.</span><button class="btn btn--secondary" id="cancelCompanyModal">Cancelar</button>${p?`<button class="btn btn--secondary" id="reanalyzeCompany" ${cr.busy?'disabled':''}>${I('refresh-cw')} Ler novamente</button><button class="btn btn--primary" id="confirmCompanyRegistration" ${!p.can_register||cr.busy?'disabled':''}>${I('building-2')} ${p.existing_company?'Confirmar vínculo':'Cadastrar empresa'}</button>`:`<button class="btn btn--primary" id="analyzeCompanyCard" ${!cr.file||cr.busy?'disabled':''}>${I('scan-line')} ${cr.busy?'Lendo...':'Ler cartão'}</button>`}</div></div></div>`;
    window.GEDIcons.render($('#modalRoot')); bindCompanyRegistrationModal();
  }

  function bindCompanyRegistrationModal(){
    const cr=state.companyRegistration; const input=$('#companyCardInput'); const dz=$('#companyCardDropzone');
    const setFile=file=>{ if(!file)return; if(file.type && file.type!=='application/pdf' && !file.name.toLowerCase().endsWith('.pdf')){toast('Arquivo inválido','Envie o Cartão CNPJ em PDF.','triangle-alert');return;} state.companyRegistration.file=file;state.companyRegistration.base64=null;state.companyRegistration.preview=null;renderCompanyRegistrationModal(); };
    $('#closeCompanyModal')?.addEventListener('click',closeModal); $('#cancelCompanyModal')?.addEventListener('click',closeModal); $('#companyModalOverlay')?.addEventListener('click',e=>{if(e.target.id==='companyModalOverlay')closeModal();});
    $('#pickCompanyCard')?.addEventListener('click',e=>{e.stopPropagation();input?.click();}); dz?.addEventListener('click',e=>{if(e.target.closest('button'))return;input?.click();}); input?.addEventListener('change',()=>setFile(input.files?.[0]));
    dz?.addEventListener('dragover',e=>{e.preventDefault();dz.classList.add('is-over');}); dz?.addEventListener('dragleave',()=>dz.classList.remove('is-over')); dz?.addEventListener('drop',e=>{e.preventDefault();dz.classList.remove('is-over');setFile(e.dataTransfer.files?.[0]);});
    $('#analyzeCompanyCard')?.addEventListener('click',analyzeCompanyCard); $('#reanalyzeCompany')?.addEventListener('click',analyzeCompanyCard); $('#confirmCompanyRegistration')?.addEventListener('click',confirmCompanyRegistration);
  }

  async function analyzeCompanyCard(){
    const cr=state.companyRegistration; if(!cr.file)return; cr.busy=true;renderCompanyRegistrationModal();
    try{ if(!cr.base64)cr.base64=await fileToBase64(cr.file); cr.preview=await api('companies/preview-card',{method:'POST',body:JSON.stringify({filename:cr.file.name,content_base64:cr.base64})}); }
    catch(e){toast('Não foi possível ler o Cartão CNPJ',e.message,'triangle-alert');cr.preview=null;}
    finally{cr.busy=false;renderCompanyRegistrationModal();}
  }

  async function confirmCompanyRegistration(){
    const cr=state.companyRegistration;if(!cr.file||!cr.preview)return;cr.busy=true;renderCompanyRegistrationModal();
    try{ if(!cr.base64)cr.base64=await fileToBase64(cr.file); const data=await api('companies/register-card',{method:'POST',body:JSON.stringify({filename:cr.file.name,content_base64:cr.base64})}); await Promise.all([loadCompanies(),loadHistory()]); closeModal();render(); toast(data.already_registered?'Empresa já cadastrada':'Empresa cadastrada',data.folder_created?'A pasta da empresa foi criada no Servidor Exemplo.':'O vínculo foi salvo e a pasta já existia.','circle-check'); }
    catch(e){toast('Cadastro não concluído',e.message,'triangle-alert');cr.busy=false;renderCompanyRegistrationModal();}
  }

  function openResult(name){
    const r=resultByName(name); if(!r)return;
    const doc=r.document||{}, company=r.company||{}, comp=r.competence||{};
    $('#modalRoot').innerHTML=`<div class="overlay" id="modalOverlay"><div class="modal modal--wide"><div class="modal__header"><div><h2>${esc(r.name)}</h2><p>Diagnóstico do reconhecedor • ${statusBadge(r.status)} <span class="confidence confidence--${r.confidence>=95?'high':r.confidence>=70?'mid':'low'}">${esc(r.confidence||0)}%</span></p></div><button class="close-button" id="closeModal">${I('x')}</button></div><div class="modal__body">
      ${r.notice?`<div class="notice">${I('triangle-alert')}<div>${esc(r.notice)}</div></div>`:''}
      ${r.duplicate_in_inbox?`<div class="notice notice--danger">${I('triangle-alert')}<div><strong>Possível duplicado:</strong> outro arquivo da AAA possui o mesmo SHA-256.</div></div>`:''}
      ${r.review_reason?`<div class="notice">${I('circle-info')}<div><strong>Motivo da revisão:</strong> ${esc(r.review_reason)}</div></div>`:''}
      <div class="diagnostic-grid">
        <div class="diagnostic"><span>Documento</span><strong>${esc(doc.name||'Não reconhecido')}</strong><small>${esc(doc.source||'—')} • ${esc(doc.confidence||0)}%</small></div>
        <div class="diagnostic"><span>Empresa</span><strong>${esc(company.folder||'Não cadastrada')}</strong><small>${esc(company.cnpj||((company.detected_cnpjs||[]).join(', ')||'CNPJ não detectado'))} • ${esc(company.source||'—')}</small></div>
        <div class="diagnostic"><span>Competência</span><strong>${esc(comp.value||'Não identificada')}</strong><small>${esc(comp.source||'—')} • ${esc(comp.confidence||0)}%</small></div>
        <div class="diagnostic"><span>Camada de texto</span><strong>${esc(r.text_chars||0)} caracteres</strong><small>${esc(r.metadata?.pages||'—')} página(s)</small></div>
      </div>
      <div class="result-section"><span>Novo nome proposto</span><code>${esc(r.proposed_name||'—')}</code></div>
      <div class="result-section"><span>Destino calculado</span><code>${esc(r.destination||'—')}</code></div>
      <div class="result-section"><span>Destino efetivo</span><code>${esc(r.final_path||'—')}</code></div>
      <div class="result-section"><span>Estrutura de pastas</span><code>${r.folder_structure?.ensured?(r.folder_structure.created?.length?`Criada/expandida: ${esc(r.folder_structure.created.join(' • '))}`:'Já existia e foi confirmada'):(r.folder_structure?.error?`Erro: ${esc(r.folder_structure.error)}`:'Não criada para este status')}</code></div>
      <div class="result-section"><span>SHA-256</span><code class="hash-code">${esc(r.hash||'—')}</code></div>
      ${doc.signatures?.length?`<div class="result-section"><span>Assinaturas encontradas</span><div class="signature-list">${doc.signatures.map(s=>`<code>${esc(s)}</code>`).join('')}</div></div>`:''}
    </div><div class="modal__footer"><span class="modal-note">v0.0.6 só arquiva automaticamente quando documento, competência e empresa cadastrada atingem a confiança mínima. Itens inseguros são desviados para REVISÃO/ERROS.</span><button class="btn btn--secondary" id="closeModalFooter">Fechar</button></div></div></div>`;
    window.GEDIcons.render($('#modalRoot'));
    $('#closeModal').onclick=closeModal; $('#closeModalFooter').onclick=closeModal; $('#modalOverlay').onclick=e=>{if(e.target.id==='modalOverlay')closeModal();};
  }
  function closeModal(){ $('#modalRoot').innerHTML=''; }

  function bindPage(){
    $$('[data-go]').forEach(el=>el.addEventListener('click',()=>go(el.dataset.go)));
    const scan=$('#scanInbox'); if(scan)scan.addEventListener('click',async()=>{await loadQueue();render();toast('AAA atualizada',`${state.queue.length} arquivo(s) encontrado(s).`,'refresh-cw');});
    $$('.js-view').forEach(b=>b.addEventListener('click',e=>{e.stopPropagation();openResult(e.currentTarget.closest('tr').dataset.resultName);}));
    $$('tr.is-clickable').forEach(tr=>tr.addEventListener('dblclick',()=>openResult(tr.dataset.resultName)));
    $$('.js-result-card').forEach(b=>b.addEventListener('click',()=>openResult(b.dataset.resultName)));
    const cs=$('#companySearch'); if(cs)cs.addEventListener('input',()=>{$('#companyRows').innerHTML=companyRows(state.companies.filter(c=>c.folder.toLowerCase().includes(cs.value.toLowerCase())||(c.cnpj||'').includes(cs.value)));});
    const rc=$('#reloadCompanies'); if(rc)rc.addEventListener('click',async()=>{await loadCompanies();render();toast('Empresas atualizadas',`${state.companies.length} pasta(s) considerada(s).`,'scan-line');});
    const reg=$('#registerCompany'); if(reg)reg.addEventListener('click',openCompanyRegistration);
    const rh=$('#reloadHistory'); if(rh)rh.addEventListener('click',async()=>{await loadHistory();render();});
    const sf=$('#settingsForm'); if(sf)sf.addEventListener('submit',async e=>{
      e.preventDefault(); const f=e.currentTarget;
      const payload={root:f.root.value,inbox:f.inbox.value,review:f.review.value,threshold:Number(f.threshold.value)||95,create_folders:f.create_folders.checked,hash_duplicates:f.hash_duplicates.checked,auto_process:f.auto_process.checked,simulation:false};
      try{const data=await api('settings',{method:'POST',body:JSON.stringify(payload)});state.settings=data.settings;await loadStatus();await Promise.all([loadQueue(),loadCompanies(),loadHistory()]);render();toast('Configurações salvas','O motor já está usando os novos parâmetros.','circle-check');}
      catch(err){toast('Não foi possível salvar',err.message,'triangle-alert');}
    });
  }

  $$('.nav__item').forEach(b=>b.addEventListener('click',()=>go(b.dataset.page)));
  $('#collapseSidebar').addEventListener('click',()=>{ const sb=$('#sidebar');sb.classList.toggle('is-collapsed');const closed=sb.classList.contains('is-collapsed');const icon=$('#collapseSidebar [data-icon]');icon.innerHTML=I(closed?'panel-left-open':'panel-left-close');storage.set('ged.sidebarCollapsed',closed); });
  $('#refreshButton').addEventListener('click',()=>refreshAll(true));
  if(storage.get('ged.sidebarCollapsed',false))$('#sidebar').classList.add('is-collapsed');
  window.GEDIcons.render(document);
  render();
  refreshAll(false);
  setInterval(async()=>{
    if(document.hidden || $('#modalRoot')?.children.length)return;
    await loadStatus();
    if(state.online) await Promise.all([loadQueue(),loadRecent(),loadHistory()]);
    if(['dashboard','inbox','review','history'].includes(state.page)) render(); else updateChrome();
  },2500);
})();
