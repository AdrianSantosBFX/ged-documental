/*
  Local SVG icon subset for GED v0.0.6.
  Visual system selected: Lucide Icons (ISC). The subset is embedded locally so the
  prototype remains usable offline and does not depend on a CDN.
*/
(function(){
  const P={
    'layout-dashboard':'<rect width="7" height="9" x="3" y="3" rx="1"/><rect width="7" height="5" x="14" y="3" rx="1"/><rect width="7" height="9" x="14" y="12" rx="1"/><rect width="7" height="5" x="3" y="16" rx="1"/>',
    'inbox':'<polyline points="22 12 16 12 14 15 10 15 8 12 2 12"/><path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/>',
    'file-search':'<path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h7"/><polyline points="14 2 14 8 20 8"/><circle cx="18" cy="18" r="3"/><path d="m20.5 20.5 2 2"/>',
    'building-2':'<path d="M6 22V4c0-.5.4-1 1-1h10c.6 0 1 .5 1 1v18"/><path d="M6 12H4c-.6 0-1 .4-1 1v9"/><path d="M18 9h2c.6 0 1 .4 1 1v12"/><path d="M10 6h4M10 10h4M10 14h4M10 18h4"/>',
    'workflow':'<rect width="8" height="8" x="3" y="3" rx="2"/><path d="M7 11v4a2 2 0 0 0 2 2h4"/><rect width="8" height="8" x="13" y="13" rx="2"/>',
    'history':'<path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5"/><path d="M12 7v5l4 2"/>',
    'settings':'<path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.38a2 2 0 0 0-.73-2.73l-.15-.09a2 2 0 0 1-1-1.74v-.51a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/>',
    'shield-check':'<path d="M20 13c0 5-3.5 7.5-8 9-4.5-1.5-8-4-8-9V5l8-3 8 3z"/><path d="m9 12 2 2 4-4"/>',
    'panel-left-close':'<rect width="18" height="18" x="3" y="3" rx="2"/><path d="M9 3v18"/><path d="m16 15-3-3 3-3"/>',
    'panel-left-open':'<rect width="18" height="18" x="3" y="3" rx="2"/><path d="M9 3v18"/><path d="m14 9 3 3-3 3"/>',
    'chevron-right':'<path d="m9 18 6-6-6-6"/>',
    'folder-open':'<path d="m6 14 1.5-2.9A2 2 0 0 1 9.3 10H20a2 2 0 0 1 1.8 2.9l-2.7 5.2A2 2 0 0 1 17.3 19H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4l2 3h6a2 2 0 0 1 2 2v2"/>',
    'refresh-cw':'<path d="M21 12a9 9 0 0 0-15.5-6.2L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 15.5 6.2L21 16"/><path d="M16 16h5v5"/>',
    'play':'<polygon points="5 3 19 12 5 21 5 3"/>',
    'file-text':'<path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5z"/><polyline points="14 2 14 8 20 8"/><path d="M8 13h8M8 17h8M8 9h2"/>',
    'clock':'<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    'circle-check':'<circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/>',
    'triangle-alert':'<path d="m21.7 18-8-14a2 2 0 0 0-3.4 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3z"/><path d="M12 9v4M12 17h.01"/>',
    'route':'<circle cx="6" cy="19" r="3"/><path d="M9 19h5.5a3.5 3.5 0 0 0 0-7h-5a3.5 3.5 0 0 1 0-7H15"/><circle cx="18" cy="5" r="3"/>',
    'folder-input':'<path d="M2 6a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2z"/><path d="M12 10v6M9 13l3 3 3-3"/>',
    'upload':'<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/>',
    'search':'<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',
    'plus':'<path d="M12 5v14M5 12h14"/>',
    'x':'<path d="M18 6 6 18M6 6l12 12"/>',
    'save':'<path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><path d="M17 21v-8H7v8M7 3v5h8"/>',
    'database':'<ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5v14c0 1.7 4 3 9 3s9-1.3 9-3V5"/><path d="M3 12c0 1.7 4 3 9 3s9-1.3 9-3"/>',
    'scan-line':'<path d="M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2M7 12h10"/>',
    'eye':'<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12"/><circle cx="12" cy="12" r="3"/>',
    'trash':'<path d="M3 6h18M8 6V4h8v2M19 6l-1 15H6L5 6M10 11v6M14 11v6"/>',
    'sliders':'<path d="M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6"/>',
    'arrow-right':'<path d="M5 12h14M13 6l6 6-6 6"/>',
    'circle-info':'<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/>',
    'activity':'<path d="M3 12h4l2-7 4 14 2-7h6"/>',
    'file-check-2':'<path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5z"/><polyline points="14 2 14 8 20 8"/><path d="m9 15 2 2 4-4"/>',
    'check':'<path d="m5 12 4 4L19 6"/>'
  };
  window.GEDIcons={
    svg(name, cls='icon'){
      const body=P[name]||P['circle-info'];
      return `<svg class="${cls}" viewBox="0 0 24 24" aria-hidden="true">${body}</svg>`;
    },
    render(root=document){
      root.querySelectorAll('[data-icon]').forEach(el=>{
        if(el.dataset.iconRendered) return;
        el.innerHTML=this.svg(el.dataset.icon);
        el.dataset.iconRendered='1';
      });
    }
  };
})();
