import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private client: Client | null = null;
  private connected$ = new BehaviorSubject<boolean>(false);
  private subscriptions: Map<string, StompSubscription> = new Map();

  constructor(private authService: AuthService) {}

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.client?.connected) {
        resolve();
        return;
      }

      const token = this.authService.getToken();
      if (!token) {
        reject('No authentication token');
        return;
      }

      this.client = new Client({
        webSocketFactory: () => new SockJS(`${environment.apiUrl}/ws?token=${token}`),
        reconnectDelay: 3000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        debug: (str) => {
          console.log('STOMP: ' + str);
        },
        onConnect: () => {
          console.log('✅ WebSocket connected');
          this.connected$.next(true);
          resolve();
        },
        onStompError: (frame) => {
          console.error('❌ STOMP error', frame);
          this.connected$.next(false);
          reject(frame);
        },
        onWebSocketClose: () => {
          console.log('⚠️ WebSocket closed, will attempt reconnect...');
          this.connected$.next(false);
        },
        onWebSocketError: (event) => {
          console.error('❌ WebSocket error:', event);
        }
      });

      this.client.activate();
    });
  }

  disconnect(): void {
    if (this.client) {
      this.subscriptions.forEach(sub => sub.unsubscribe());
      this.subscriptions.clear();
      this.client.deactivate();
      this.connected$.next(false);
    }
  }

  subscribe<T>(destination: string, callback: (data: T) => void): void {
    if (!this.client?.connected) {
      console.warn('WebSocket not connected, attempting to connect...');
      this.connect().then(() => this.subscribe(destination, callback));
      return;
    }

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error('Error parsing message', error);
      }
    });

    this.subscriptions.set(destination, subscription);
  }

  unsubscribe(destination: string): void {
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
    }
  }

  send(destination: string, body: any = {}): void {
    if (!this.client?.connected) {
      console.error('❌ Cannot send message, WebSocket not connected');
      return;
    }

    console.log('📤 Sending to:', destination, 'Body:', body);
    this.client.publish({
      destination,
      body: JSON.stringify(body)
    });
    console.log('✅ Message sent');
  }

  isConnected(): Observable<boolean> {
    return this.connected$.asObservable();
  }
}


