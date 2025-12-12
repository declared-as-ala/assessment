import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    
    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should register a new user', () => {
    const request: RegisterRequest = {
      email: 'test@example.com',
      password: 'Password1!',
      displayName: 'Test User'
    };

    const mockResponse: AuthResponse = {
      accessToken: 'test-token',
      tokenType: 'Bearer',
      user: {
        id: 1,
        email: 'test@example.com',
        displayName: 'Test User',
        online: true
      }
    };

    service.register(request).subscribe(response => {
      expect(response).toEqual(mockResponse);
      expect(service.currentUser()).toEqual(mockResponse.user);
      expect(localStorage.getItem('chess_token')).toBe('test-token');
    });

    const req = httpMock.expectOne('http://localhost:8080/api/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should login user', () => {
    const request: LoginRequest = {
      email: 'test@example.com',
      password: 'Password1!'
    };

    const mockResponse: AuthResponse = {
      accessToken: 'test-token',
      tokenType: 'Bearer',
      user: {
        id: 1,
        email: 'test@example.com',
        displayName: 'Test User',
        online: true
      }
    };

    service.login(request).subscribe(response => {
      expect(response).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should logout user', () => {
    localStorage.setItem('chess_token', 'test-token');
    localStorage.setItem('chess_user', JSON.stringify({ id: 1, email: 'test@example.com' }));

    service.logout();

    expect(localStorage.getItem('chess_token')).toBeNull();
    expect(localStorage.getItem('chess_user')).toBeNull();
    expect(service.currentUser()).toBeNull();
  });

  it('should check if user is authenticated', () => {
    expect(service.isAuthenticated()).toBe(false);

    localStorage.setItem('chess_token', 'test-token');
    expect(service.isAuthenticated()).toBe(true);
  });
});



