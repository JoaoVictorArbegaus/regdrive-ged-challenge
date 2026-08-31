import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from './auth.service';
import { DocumentItem, DocumentService, errorMessage } from './document.service';

@Component({
  selector: 'app-documents',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './documents.html'
})
export class Documents implements OnInit {
  readonly auth = inject(AuthService);
  private readonly documents = inject(DocumentService);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);

  items: DocumentItem[] = [];
  title = '';
  tag = '';
  status = '';
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = false;
  error = '';
  showCreate = false;
  newTitle = '';
  newDescription = '';
  newTags = '';

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    this.loading = true;
    this.error = '';
    this.documents.list({
      title: this.title.trim(),
      tag: this.tag.trim(),
      status: this.status,
      page,
      size: 10
    }).pipe(finalize(() => {
      this.loading = false;
      this.changeDetector.markForCheck();
    }))
      .subscribe({
        next: (response) => {
          this.items = response.content;
          this.page = response.page;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
        },
        error: (error) => this.error = errorMessage(error)
      });
  }

  clearFilters(): void {
    this.title = '';
    this.tag = '';
    this.status = '';
    this.load();
  }

  create(): void {
    if (!this.newTitle.trim()) {
      this.error = 'Informe o titulo do documento.';
      return;
    }
    this.documents.create({
      title: this.newTitle.trim(),
      description: this.newDescription.trim() || null,
      tags: this.parseTags(this.newTags)
    }).subscribe({
      next: (document) => this.router.navigate(['/documents', document.id]),
      error: (error) => {
        this.error = errorMessage(error);
        this.changeDetector.markForCheck();
      }
    });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }

  canWrite(): boolean {
    return this.auth.user()?.role !== 'VIEWER';
  }

  private parseTags(value: string): string[] {
    return value.split(',').map((tag) => tag.trim()).filter((tag) => tag.length > 0);
  }
}
