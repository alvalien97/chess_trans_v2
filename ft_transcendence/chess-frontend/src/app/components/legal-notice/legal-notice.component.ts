// ElementRef permite trabajar con un elemento HTML; EventEmitter comunica acciones al componente padre.
import { AfterViewInit, Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';

// Solo se aceptan estas dos secciones legales en la propiedad page.
export type LegalPage = 'privacy' | 'terms';

@Component({
  // Etiqueta Angular que representa esta pantalla legal.
  selector: 'app-legal-notice',
  // Declara explícitamente que este componente puede usarse de forma independiente.
  standalone: true,
  // Contenido y estilos de la pantalla.
  templateUrl: './legal-notice.component.html',
  styleUrl: './legal-notice.component.css'
})
export class LegalNoticeComponent implements AfterViewInit {
  // Guarda una referencia al título para mover allí el foco al abrir el documento.
  @ViewChild('pageHeading') pageHeading?: ElementRef<HTMLHeadingElement>;
  // El componente padre debe indicar qué documento mostrar.
  @Input({ required: true }) page!: LegalPage;
  // Evento que pide al padre cerrar la pantalla legal.
  @Output() close = new EventEmitter<void>();
  // Evento que pide al padre cambiar entre privacidad y términos.
  @Output() navigate = new EventEmitter<LegalPage>();

  // Se ejecuta cuando Angular ya ha creado el contenido HTML de la plantilla.
  ngAfterViewInit() {
    // Enfoca el título para orientar a quienes usan teclado o lector de pantalla.
    if (typeof window !== 'undefined') {
      this.pageHeading?.nativeElement.focus();
    }
  }
}