import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export type DocumentStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface DocumentItem {
  id: string;
  title: string;
  description: string | null;
  tags: string[];
  status: DocumentStatus;
  tenantId: string;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentPage {
  content: DocumentItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DocumentVersion {
  id: string;
  documentId: string;
  versionNumber: number;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
  checksum: string;
  uploadedAt: string;
  uploadedBy: string;
}

export interface AuditEvent {
  id: string;
  documentId: string;
  userId: string;
  action: string;
  metadata: Record<string, unknown>;
  timestamp: string;
}

export interface ProblemDetail {
  detail?: string;
  title?: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly http = inject(HttpClient);

  list(filters: {
    title?: string;
    tag?: string;
    status?: string;
    page: number;
    size: number;
  }) {
    let params = new HttpParams()
      .set('page', filters.page)
      .set('size', filters.size)
      .set('sort', 'createdAt,desc');
    if (filters.title) {
      params = params.set('title', filters.title);
    }
    if (filters.tag) {
      params = params.set('tag', filters.tag);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    return this.http.get<DocumentPage>('/api/documents', { params });
  }

  findById(id: string) {
    return this.http.get<DocumentItem>(`/api/documents/${id}`);
  }

  create(data: { title: string; description: string | null; tags: string[] }) {
    return this.http.post<DocumentItem>('/api/documents', data);
  }

  update(id: string, data: { title: string; description: string | null; tags: string[] }) {
    return this.http.put<DocumentItem>(`/api/documents/${id}`, data);
  }

  updateStatus(id: string, status: DocumentStatus) {
    return this.http.patch<DocumentItem>(`/api/documents/${id}/status`, { status });
  }

  versions(id: string) {
    return this.http.get<DocumentVersion[]>(`/api/documents/${id}/versions`);
  }

  upload(id: string, file: File) {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<DocumentVersion>(`/api/documents/${id}/versions`, body);
  }

  download(id: string, versionNumber: number) {
    return this.http.get(`/api/documents/${id}/versions/${versionNumber}/download`, {
      responseType: 'blob'
    });
  }

  audit(id: string) {
    return this.http.get<AuditEvent[]>(`/api/documents/${id}/audit`);
  }
}

export function errorMessage(error: unknown): string {
  if (error && typeof error === 'object' && 'error' in error) {
    const body = (error as { error?: ProblemDetail }).error;
    if (body?.detail) {
      return body.detail;
    }
  }
  return 'Nao foi possivel concluir a operacao.';
}
