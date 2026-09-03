import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-product-wizard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './product-wizard.component.html',
  styleUrls: ['./product-wizard.component.scss']
})
export class ProductWizardComponent implements OnInit {
  wizardForm!: FormGroup;
  currentStep = 1;
  totalSteps = 10;
  isSavingDraft = false;
  draftProductId: string | null = null;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.initForm();
  }

  initForm(): void {
    this.wizardForm = this.fb.group({
      step1: this.fb.group({
        productName: ['', Validators.required],
        description: ['', Validators.required],
        categoryId: ['', Validators.required],
      }),
      step2: this.fb.group({
        basePrice: ['', [Validators.required, Validators.min(0)]],
        discountPrice: [''],
      }),
      step3: this.fb.group({
        initialStock: ['', [Validators.required, Validators.min(0)]],
        lowStockThreshold: [''],
      }),
      step4: this.fb.group({
        brand: [''],
        countryOfOrigin: [''],
        condition: ['NEW', Validators.required],
      }),
      step5: this.fb.group({
        // Media placeholder
      }),
      step6: this.fb.group({
        // Variants placeholder
      }),
      step7: this.fb.group({
        weightGrams: [''],
        lengthCm: [''],
        widthCm: [''],
        heightCm: ['']
      }),
      step8: this.fb.group({
        metaKeywords: ['']
      }),
      step9: this.fb.group({
        returnPolicyId: [''],
        warrantyDuration: ['']
      }),
      step10: this.fb.group({
        // Final Review
      })
    });
  }

  get currentStepGroup(): FormGroup {
    return this.wizardForm.get(`step${this.currentStep}`) as FormGroup;
  }

  nextStep(): void {
    if (this.currentStepGroup.valid) {
      this.saveDraft();
      if (this.currentStep < this.totalSteps) {
        this.currentStep++;
      }
    } else {
      this.currentStepGroup.markAllAsTouched();
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  saveDraft(): void {
    this.isSavingDraft = true;
    // Simulate API call for auto-saving draft
    setTimeout(() => {
      this.isSavingDraft = false;
      if (!this.draftProductId) {
        this.draftProductId = 'PRD-DRAFT-123'; // Mock ID
      }
    }, 1000);
  }

  submitProduct(): void {
    if (this.wizardForm.valid) {
      console.log('Submitting Product for Review...', this.wizardForm.value);
      // Integration with Backend to mark PENDING_REVIEW
    } else {
      this.wizardForm.markAllAsTouched();
    }
  }
}
