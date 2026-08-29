import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-product-ai-insights',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ai-tab">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:32px;">
        <div>
          <h2 style="margin:0 0 8px 0; color:#fff; display:flex; align-items:center; gap:12px;">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color:#ec4899;"><path d="M12 2a10 10 0 1 0 10 10H12V2z"></path><path d="M21.18 8.02c-1-2.3-2.85-4.17-5.16-5.18"></path><path d="M21.18 15.98a10.02 10.02 0 0 1-9.2 5.92"></path></svg>
            Cognitive AI Insights
          </h2>
          <p style="margin:0; color:var(--text-muted); font-size:14px;">AI-driven analysis of product listing quality, pricing, and policy compliance.</p>
        </div>
        <button class="btn btn-ai" (click)="analyze()">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" [class.spin]="analyzing"><path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/></svg>
          {{ analyzing ? 'Analyzing...' : 'Run Full Analysis' }}
        </button>
      </div>

      <div class="insights-grid" *ngIf="!analyzing">
        
        <!-- Score Card -->
        <div class="insight-card score-card">
          <div class="score-circle">
            <svg viewBox="0 0 36 36" class="circular-chart">
              <path class="circle-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
              <path class="circle" stroke-dasharray="85, 100" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
              <text x="18" y="20.35" class="percentage">85</text>
            </svg>
          </div>
          <h3>Listing Quality Score</h3>
          <p>Very Good. Minor optimizations suggested.</p>
        </div>

        <div class="recommendations">
          
          <div class="rec-item success">
            <div class="icon"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg></div>
            <div class="content">
              <h4>Return Policy Optimal</h4>
              <p>The current return policy matches category expectations. Products in this category with this policy see 14% higher conversion.</p>
            </div>
          </div>

          <div class="rec-item warning">
            <div class="icon"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg></div>
            <div class="content">
              <h4>Pricing Competitiveness</h4>
              <p>Base price (₹{{product?.basePrice}}) is slightly above market average (₹{{product?.basePrice * 0.9 | number:'1.0-0'}}). Consider adding a minor discount.</p>
            </div>
          </div>

          <div class="rec-item info">
            <div class="icon"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg></div>
            <div class="content">
              <h4>SEO & Description</h4>
              <p>Description length is adequate, but lacks key search phrases like "sustainable" or "eco-friendly".</p>
            </div>
          </div>

        </div>
      </div>
      
      <div class="analyzing-state" *ngIf="analyzing">
        <div class="pulse-ring"></div>
        <h3>Cognitive Core Processing...</h3>
        <p>Scanning market data, NLP on description, and analyzing image quality.</p>
      </div>

    </div>
  `,
  styles: [`
    .ai-tab { animation: fadeIn 0.3s ease; }
    
    .btn { display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; border: none; }
    .btn-ai { background: linear-gradient(135deg, #ec4899 0%, #8b5cf6 100%); color: #fff; box-shadow: 0 4px 15px rgba(236,72,153,0.3); }
    .btn-ai:hover { box-shadow: 0 6px 20px rgba(236,72,153,0.5); transform: translateY(-1px); }
    .spin { animation: spin 1s linear infinite; }
    
    .insights-grid { display: grid; grid-template-columns: 300px 1fr; gap: 32px; }
    @media (max-width: 900px) { .insights-grid { grid-template-columns: 1fr; } }
    
    .insight-card { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.05); border-radius: 16px; padding: 32px; text-align: center; }
    
    /* Circular Chart */
    .score-circle { width: 150px; margin: 0 auto 24px auto; }
    .circular-chart { display: block; margin: 0 auto; max-width: 100%; max-height: 250px; }
    .circle-bg { fill: none; stroke: rgba(255,255,255,0.05); stroke-width: 2.5; }
    .circle { fill: none; stroke: #10b981; stroke-width: 2.5; stroke-linecap: round; animation: progress 1s ease-out forwards; }
    .percentage { fill: #fff; font-family: var(--font-heading); font-size: 8px; font-weight: bold; text-anchor: middle; }
    
    .score-card h3 { margin: 0 0 8px 0; font-size: 18px; color: #fff; }
    .score-card p { margin: 0; font-size: 13px; color: var(--text-muted); }
    
    .recommendations { display: flex; flex-direction: column; gap: 16px; }
    .rec-item { display: flex; gap: 16px; padding: 20px; border-radius: 12px; background: rgba(0,0,0,0.2); border-left: 4px solid; align-items: flex-start; }
    .rec-item.success { border-color: #10b981; background: linear-gradient(90deg, rgba(16,185,129,0.05) 0%, transparent 100%); }
    .rec-item.success .icon { color: #10b981; background: rgba(16,185,129,0.1); }
    .rec-item.warning { border-color: #f59e0b; background: linear-gradient(90deg, rgba(245,158,11,0.05) 0%, transparent 100%); }
    .rec-item.warning .icon { color: #f59e0b; background: rgba(245,158,11,0.1); }
    .rec-item.info { border-color: #3b82f6; background: linear-gradient(90deg, rgba(59,130,246,0.05) 0%, transparent 100%); }
    .rec-item.info .icon { color: #3b82f6; background: rgba(59,130,246,0.1); }
    
    .rec-item .icon { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
    .content h4 { margin: 0 0 6px 0; font-size: 15px; color: #fff; }
    .content p { margin: 0; font-size: 13px; color: var(--text-dim); line-height: 1.5; }

    .analyzing-state { text-align: center; padding: 60px 0; }
    .pulse-ring { width: 60px; height: 60px; border-radius: 50%; background: rgba(236,72,153,0.2); margin: 0 auto 24px auto; position: relative; }
    .pulse-ring::after { content: ''; position: absolute; inset: 0; border-radius: 50%; box-shadow: 0 0 20px rgba(236,72,153,0.5); animation: pulse 1.5s infinite; }
    .analyzing-state h3 { color: #ec4899; margin: 0 0 8px 0; }
    .analyzing-state p { color: var(--text-muted); font-size: 14px; }

    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
    @keyframes progress { 0% { stroke-dasharray: 0 100; } }
    @keyframes spin { 100% { transform: rotate(360deg); } }
    @keyframes pulse { 0% { transform: scale(1); opacity: 1; } 100% { transform: scale(1.5); opacity: 0; } }
  `]
})
export class ProductAiInsightsComponent {
  @Input() product: any;
  analyzing = false;

  analyze() {
    this.analyzing = true;
    setTimeout(() => {
      this.analyzing = false;
    }, 2500);
  }
}
