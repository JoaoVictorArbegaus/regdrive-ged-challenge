import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from './auth.service';
import { errorMessage } from './document.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html'
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);

  username = '';
  password = '';
  loading = false;
  error = '';

  submit(): void {
    if (!this.username.trim() || !this.password) {
      this.error = 'Informe usuario e senha.';
      return;
    }
    this.loading = true;
    this.error = '';
    this.auth.login(this.username.trim(), this.password)
      .pipe(finalize(() => {
        this.loading = false;
        this.changeDetector.markForCheck();
      }))
      .subscribe({
        next: () => this.router.navigateByUrl('/documents'),
        error: (error) => this.error = errorMessage(error)
      });
  }
}
