import { useRef, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { ApiError, errorMessage, supportOrdersApi } from '../api/supportOrders'
import { CATEGORIES, CATEGORY_LABELS, CHANNELS, CHANNEL_LABELS, PRIORITIES, PRIORITY_LABELS } from '../api/types'
import type { Category, Channel, Priority, SupportOrderDetail } from '../api/types'
import { DESCRIPTION_MAX, DESCRIPTION_MIN, EMPTY_FORM, validateOrderForm } from './orderFormValidation'
import type { OrderFormErrors, OrderFormValues } from './orderFormValidation'

interface CreateOrderFormProps {
  onCreated: (order: SupportOrderDetail) => void
  onCancel: () => void
}

export function CreateOrderForm({ onCreated, onCancel }: CreateOrderFormProps) {
  const [values, setValues] = useState<OrderFormValues>(EMPTY_FORM)
  const [touched, setTouched] = useState<Partial<Record<keyof OrderFormValues, boolean>>>({})
  const [submitted, setSubmitted] = useState(false)
  const [serverErrors, setServerErrors] = useState<OrderFormErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const formRef = useRef<HTMLFormElement>(null)

  const clientErrors = validateOrderForm(values)
  const visibleError = (field: keyof OrderFormValues) =>
    serverErrors[field] ?? (submitted || touched[field] ? clientErrors[field] : undefined)

  function update<K extends keyof OrderFormValues>(field: K, value: OrderFormValues[K]) {
    setValues((current) => ({ ...current, [field]: value }))
    setServerErrors((current) => ({ ...current, [field]: undefined }))
  }

  const markTouched = (field: keyof OrderFormValues) => setTouched((current) => ({ ...current, [field]: true }))

  const a11y = (field: keyof OrderFormValues) => ({
    'aria-invalid': Boolean(visibleError(field)),
    'aria-describedby': visibleError(field) ? `${field}-error` : undefined,
  })

  /** Tras un envío fallido lleva el foco al primer campo inválido o, si no hay, al mensaje de error. */
  function focusFirstProblem() {
    // setTimeout: se ejecuta después de que React aplique los errores al DOM.
    window.setTimeout(() => {
      const form = formRef.current
      const target = form?.querySelector<HTMLElement>('[aria-invalid="true"]') ?? form?.querySelector<HTMLElement>('.alert')
      target?.focus()
    }, 0)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitted(true)
    setFormError(null)
    if (Object.keys(clientErrors).length > 0) {
      focusFirstProblem()
      return
    }

    setSubmitting(true)
    try {
      const created = await supportOrdersApi.create({
        externalReference: values.externalReference.trim(),
        customerId: values.customerId.trim(),
        category: values.category as Category,
        description: values.description.trim(),
        channel: values.channel as Channel,
        priority: values.priority,
      })
      onCreated(created)
    } catch (error) {
      if (error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) {
        setServerErrors(error.fieldErrors as OrderFormErrors)
      }
      setFormError(errorMessage(error))
      focusFirstProblem()
    } finally {
      setSubmitting(false)
    }
  }

  const descriptionLength = values.description.trim().length

  return (
    <section className="card" aria-labelledby="create-title">
      <div className="card-header">
        <h2 id="create-title">Nueva orden</h2>
        <button type="button" className="button-secondary" onClick={onCancel}>
          Cerrar
        </button>
      </div>

      <form ref={formRef} className="form" onSubmit={handleSubmit} noValidate>
        {formError && (
          <div className="alert alert-error" role="alert" tabIndex={-1}>
            {formError}
          </div>
        )}

        <Field id="externalReference" label="Referencia externa" error={visibleError('externalReference')}>
          <input
            id="externalReference"
            {...a11y('externalReference')}
            value={values.externalReference}
            placeholder="MOB-1001"
            maxLength={50}
            onChange={(event) => update('externalReference', event.target.value)}
            onBlur={() => markTouched('externalReference')}
          />
        </Field>

        <Field id="customerId" label="Cliente" error={visibleError('customerId')}>
          <input
            id="customerId"
            {...a11y('customerId')}
            value={values.customerId}
            placeholder="CUST-001"
            maxLength={50}
            onChange={(event) => update('customerId', event.target.value)}
            onBlur={() => markTouched('customerId')}
          />
        </Field>

        <div className="form-row">
          <Field id="category" label="Categoría" error={visibleError('category')}>
            <select
              id="category"
              {...a11y('category')}
              value={values.category}
              onChange={(event) => update('category', event.target.value as Category | '')}
              onBlur={() => markTouched('category')}
            >
              <option value="">Selecciona…</option>
              {CATEGORIES.map((category) => (
                <option key={category} value={category}>
                  {CATEGORY_LABELS[category]}
                </option>
              ))}
            </select>
          </Field>

          <Field id="channel" label="Canal" error={visibleError('channel')}>
            <select
              id="channel"
              {...a11y('channel')}
              value={values.channel}
              onChange={(event) => update('channel', event.target.value as Channel | '')}
              onBlur={() => markTouched('channel')}
            >
              {CHANNELS.map((channel) => (
                <option key={channel} value={channel}>
                  {CHANNEL_LABELS[channel]}
                </option>
              ))}
            </select>
          </Field>

          <Field id="priority" label="Prioridad" error={visibleError('priority')}>
            <select
              id="priority"
              {...a11y('priority')}
              value={values.priority}
              onChange={(event) => update('priority', event.target.value as Priority)}
            >
              {PRIORITIES.map((priority) => (
                <option key={priority} value={priority}>
                  {PRIORITY_LABELS[priority]}
                </option>
              ))}
            </select>
          </Field>
        </div>

        <Field
          id="description"
          label="Descripción"
          error={visibleError('description')}
          hint={`${descriptionLength}/${DESCRIPTION_MAX} · mínimo ${DESCRIPTION_MIN}`}
        >
          <textarea
            id="description"
            {...a11y('description')}
            rows={4}
            value={values.description}
            maxLength={DESCRIPTION_MAX + 50}
            onChange={(event) => update('description', event.target.value)}
            onBlur={() => markTouched('description')}
          />
        </Field>

        <div className="form-actions">
          <button type="submit" disabled={submitting}>
            {submitting ? 'Creando…' : 'Crear orden'}
          </button>
        </div>
      </form>
    </section>
  )
}

interface FieldProps {
  id: string
  label: string
  error?: string
  hint?: string
  children: ReactNode
}

/** Etiqueta + control + mensaje de error (el control lo referencia con aria-describedby). */
function Field({ id, label, error, hint, children }: FieldProps) {
  return (
    <div className={`field${error ? ' has-error' : ''}`}>
      <label htmlFor={id}>{label}</label>
      {children}
      {hint && <small className="hint">{hint}</small>}
      {error && (
        <small id={`${id}-error`} className="field-error" role="alert">
          {error}
        </small>
      )}
    </div>
  )
}
