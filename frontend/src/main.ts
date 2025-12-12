import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

// Polyfill for SockJS/STOMP (required for WebSocket)
(window as any).global = window;
(window as any).process = { 
  env: { DEBUG: undefined },
  version: '',
  nextTick: (fn: any) => setTimeout(fn, 0)
};
(window as any).Buffer = (window as any).Buffer || {
  isBuffer: () => false
};

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));


