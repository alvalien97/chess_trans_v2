import { AfterViewInit, Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';

export type LegalPage = 'privacy' | 'terms';

@Component({
  selector: 'app-legal-notice',
  standalone: true,
  templateUrl: './legal-notice.component.html',
  styleUrl: './legal-notice.component.css'
})
export class LegalNoticeComponent implements AfterViewInit {
  @ViewChild('pageHeading') pageHeading?: ElementRef<HTMLHeadingElement>;
  @Input({ required: true }) page!: LegalPage;
  @Output() close = new EventEmitter<void>();
  @Output() navigate = new EventEmitter<LegalPage>();

  ngAfterViewInit() {
    if (typeof window !== 'undefined') {
      this.pageHeading?.nativeElement.focus();
    }
  }
}