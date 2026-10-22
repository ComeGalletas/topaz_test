import type { Category, Channel, Priority } from '../api/types'

export interface OrderFormValues {
  externalReference: string
  customerId: string
  category: Category | ''
  description: string
  channel: Channel | ''
  priority: Priority
}

export type OrderFormErrors = Partial<Record<keyof OrderFormValues, string>>

export const DESCRIPTION_MIN = 10
export const DESCRIPTION_MAX = 500
const ID_MAX = 50

export const EMPTY_FORM: OrderFormValues = {
  externalReference: '',
  customerId: '',
  category: '',
  description: '',
  channel: 'MOBILE_APP',
  priority: 'MEDIUM',
}

/** Mismas reglas que el backend, para avisar antes de enviar. El backend sigue siendo la fuente de verdad. */
export function validateOrderForm(values: OrderFormValues): OrderFormErrors {
  const errors: OrderFormErrors = {}
  const externalReference = values.externalReference.trim()
  const customerId = values.customerId.trim()
  const description = values.description.trim()

  if (!externalReference) errors.externalReference = 'La referencia externa es obligatoria.'
  else if (externalReference.length > ID_MAX) errors.externalReference = `Máximo ${ID_MAX} caracteres.`

  if (!customerId) errors.customerId = 'El cliente es obligatorio.'
  else if (customerId.length > ID_MAX) errors.customerId = `Máximo ${ID_MAX} caracteres.`

  if (!values.category) errors.category = 'Selecciona una categoría.'
  if (!values.channel) errors.channel = 'Selecciona un canal.'

  if (!description) errors.description = 'La descripción es obligatoria.'
  else if (description.length < DESCRIPTION_MIN || description.length > DESCRIPTION_MAX) {
    errors.description = `Debe tener entre ${DESCRIPTION_MIN} y ${DESCRIPTION_MAX} caracteres.`
  }

  return errors
}
