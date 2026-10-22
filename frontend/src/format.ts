const dateFormatter = new Intl.DateTimeFormat('es', { dateStyle: 'short', timeStyle: 'short' })

export function formatDate(iso: string): string {
  return dateFormatter.format(new Date(iso))
}
