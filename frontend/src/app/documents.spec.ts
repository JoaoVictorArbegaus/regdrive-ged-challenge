import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { AuthService } from './auth.service';
import { DocumentPage, DocumentService } from './document.service';
import { Documents } from './documents';

describe('Documents', () => {
  it('renders documents received after the initial request', async () => {
    const response = new Subject<DocumentPage>();
    await TestBed.configureTestingModule({
      imports: [Documents],
      providers: [
        provideRouter([]),
        { provide: DocumentService, useValue: { list: () => response.asObservable() } },
        {
          provide: AuthService,
          useValue: {
            user: signal({ username: 'user', role: 'USER', tenantId: 'tenant-demo' }),
            logout: () => undefined
          }
        }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(Documents);
    fixture.detectChanges();

    response.next({
      content: [{
        id: 'document-id',
        title: 'Contrato carregado',
        description: null,
        tags: ['legal'],
        status: 'DRAFT',
        tenantId: 'tenant-demo',
        ownerId: 'user-id',
        createdAt: '2026-08-31T12:00:00Z',
        updatedAt: '2026-08-31T12:00:00Z'
      }],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1
    });
    response.complete();
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Contrato carregado');
  });
});
