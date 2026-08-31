import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from './auth.service';
import {
  AuditEvent,
  DocumentItem,
  DocumentService,
  DocumentStatus,
  DocumentVersion,
  errorMessage
} from './document.service';

@Component({
  selector: 'app-document-detail',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './document-detail.html'
})
export class DocumentDetail implements OnInit {
  readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly documents = inject(DocumentService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  readonly documentId = this.route.snapshot.paramMap.get('id') ?? '';
  document: DocumentItem | null = null;
  versions: DocumentVersion[] = [];
  events: AuditEvent[] = [];
  title = '';
  description = '';
  tags = '';
  selectedFile: File | null = null;
  loading = true;
  error = '';
  success = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    forkJoin({
      document: this.documents.findById(this.documentId),
      versions: this.documents.versions(this.documentId),
      events: this.documents.audit(this.documentId)
    }).subscribe({
      next: (response) => {
        this.document = response.document;
        this.versions = response.versions;
        this.events = response.events;
        this.title = response.document.title;
        this.description = response.document.description ?? '';
        this.tags = response.document.tags.join(', ');
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.error = errorMessage(error);
        this.loading = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  saveMetadata(): void {
    if (!this.title.trim()) {
      this.error = 'Informe o titulo.';
      return;
    }
    this.documents.update(this.documentId, {
      title: this.title.trim(),
      description: this.description.trim() || null,
      tags: this.parseTags(this.tags)
    }).subscribe({
      next: () => {
        this.success = 'Metadados atualizados.';
        this.load();
      },
      error: (error) => this.showError(error)
    });
  }

  changeStatus(status: DocumentStatus): void {
    this.documents.updateStatus(this.documentId, status).subscribe({
      next: () => {
        if (status === 'PUBLISHED') {
          this.success = 'Documento publicado.';
        } else {
          this.success = 'Documento arquivado.';
        }
        this.load();
      },
      error: (error) => this.showError(error)
    });
  }

  chooseFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  upload(): void {
    if (!this.selectedFile) {
      this.error = 'Selecione um arquivo.';
      return;
    }
    this.documents.upload(this.documentId, this.selectedFile).subscribe({
      next: () => {
        this.selectedFile = null;
        this.success = 'Nova versao enviada.';
        this.load();
      },
      error: (error) => this.showError(error)
    });
  }

  download(version: DocumentVersion): void {
    this.documents.download(this.documentId, version.versionNumber).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = window.document.createElement('a');
        anchor.href = url;
        anchor.download = version.originalFilename;
        anchor.click();
        URL.revokeObjectURL(url);
        this.load();
      },
      error: (error) => this.showError(error)
    });
  }

  canWrite(): boolean {
    return this.auth.user()?.role !== 'VIEWER' && this.document?.status !== 'ARCHIVED';
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    return `${(bytes / 1024).toFixed(1)} KiB`;
  }

  private parseTags(value: string): string[] {
    return value.split(',').map((tag) => tag.trim()).filter((tag) => tag.length > 0);
  }

  private showError(error: unknown): void {
    this.error = errorMessage(error);
    this.changeDetector.markForCheck();
  }
}
