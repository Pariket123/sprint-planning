import {
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type FocusEvent,
  type KeyboardEvent,
  type MouseEvent,
  type TextareaHTMLAttributes,
} from 'react'
import {
  getJqlReferenceData,
  getJqlSuggestions,
  type JqlReferenceDataDto,
  type JqlSuggestionItemDto,
} from '../../api/jqlApi'
import {
  applyJqlSuggestion,
  buildLocalJqlSuggestions,
  resolveJqlClauseContext,
  type JqlSuggestionOption,
} from '../../utils/jqlAutocomplete'

interface JqlAutocompleteTextareaProps
  extends Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'value' | 'onChange'> {
  podId: string
  value: string
  onChange: (value: string) => void
}

const referenceCache = new Map<string, Promise<JqlReferenceDataDto>>()

function loadReference(podId: string): Promise<JqlReferenceDataDto> {
  const cached = referenceCache.get(podId)
  if (cached) {
    return cached
  }

  const request = getJqlReferenceData(podId).catch((error: unknown) => {
    referenceCache.delete(podId)
    throw error
  })
  referenceCache.set(podId, request)
  return request
}

export function JqlAutocompleteTextarea({
  podId,
  value,
  onChange,
  onFocus,
  onBlur,
  onKeyDown,
  className,
  ...textareaProps
}: JqlAutocompleteTextareaProps) {
  const listboxId = useId()
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const optionRefs = useRef<Array<HTMLLIElement | null>>([])
  const [reference, setReference] = useState<JqlReferenceDataDto | null>(null)
  const [suggestions, setSuggestions] = useState<JqlSuggestionOption[]>([])
  const [activeIndex, setActiveIndex] = useState(0)
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const debounceRef = useRef<number | null>(null)

  useEffect(() => {
    return () => {
      if (debounceRef.current != null) {
        window.clearTimeout(debounceRef.current)
      }
    }
  }, [])

  useEffect(() => {
    if (!open || suggestions.length === 0) {
      return
    }

    const activeOption = optionRefs.current[activeIndex]
    if (!activeOption) {
      return
    }

    activeOption.scrollIntoView({ block: 'nearest' })
  }, [activeIndex, open, suggestions])

  const ensureReference = useCallback(async () => {
    if (reference) {
      return reference
    }
    const data = await loadReference(podId)
    setReference(data)
    return data
  }, [podId, reference])

  const refreshSuggestions = useCallback(
    async (query: string, cursor: number, refData: JqlReferenceDataDto | null) => {
      const context = resolveJqlClauseContext(query, cursor, refData)
      const localSuggestions = buildLocalJqlSuggestions(context, refData)

      if (context.kind === 'value' && context.field?.value) {
        setLoading(true)
        setOpen(true)
        try {
          const remote = await getJqlSuggestions(podId, context.field.value, context.partial)
          const remoteSuggestions: JqlSuggestionOption[] = (remote.results ?? []).map(
            (item: JqlSuggestionItemDto) => ({
              kind: 'value',
              value: item.value,
              label: item.displayName.replace(/<[^>]*>/g, ''),
            }),
          )
          const nextSuggestions =
            remoteSuggestions.length > 0 ? remoteSuggestions : localSuggestions
          setSuggestions(nextSuggestions)
          setOpen(nextSuggestions.length > 0)
        } catch {
          setSuggestions(localSuggestions)
          setOpen(localSuggestions.length > 0)
        } finally {
          setLoading(false)
        }
      } else {
        setSuggestions(localSuggestions)
        setOpen(localSuggestions.length > 0)
      }

      setActiveIndex(0)
      optionRefs.current = []
    },
    [podId],
  )

  const scheduleSuggestions = useCallback(
    (query: string, cursor: number) => {
      if (debounceRef.current != null) {
        window.clearTimeout(debounceRef.current)
      }

      debounceRef.current = window.setTimeout(() => {
        void (async () => {
          try {
            const refData = await ensureReference()
            await refreshSuggestions(query, cursor, refData)
          } catch {
            setSuggestions([])
            setOpen(false)
          }
        })()
      }, 200)
    },
    [ensureReference, refreshSuggestions],
  )

  const handleChange = (nextValue: string) => {
    onChange(nextValue)
    const cursor = textareaRef.current?.selectionStart ?? nextValue.length
    scheduleSuggestions(nextValue, cursor)
  }

  const handleFocus = (event: FocusEvent<HTMLTextAreaElement>) => {
    onFocus?.(event)
    const cursor = event.currentTarget.selectionStart ?? value.length
    scheduleSuggestions(value, cursor)
  }

  const handleBlur = (event: FocusEvent<HTMLTextAreaElement>) => {
    onBlur?.(event)
    window.setTimeout(() => setOpen(false), 150)
  }

  const selectSuggestion = (suggestion: JqlSuggestionOption) => {
    const textarea = textareaRef.current
    const cursor = textarea?.selectionStart ?? value.length
    const refData = reference
    const context = resolveJqlClauseContext(value, cursor, refData)
    const { nextValue, nextCursor } = applyJqlSuggestion(value, context, suggestion)
    onChange(nextValue)
    setOpen(false)

    window.requestAnimationFrame(() => {
      if (!textareaRef.current) {
        return
      }
      textareaRef.current.focus()
      textareaRef.current.setSelectionRange(nextCursor, nextCursor)
      scheduleSuggestions(nextValue, nextCursor)
    })
  }

  const handleListMouseMove = (event: MouseEvent<HTMLUListElement>) => {
    const option = (event.target as HTMLElement).closest('[data-option-index]')
    if (!option) {
      return
    }

    const index = Number(option.getAttribute('data-option-index'))
    if (!Number.isNaN(index)) {
      setActiveIndex(index)
    }
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    onKeyDown?.(event)

    if (!open || suggestions.length === 0) {
      if (event.key === 'Escape') {
        setOpen(false)
      }
      return
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setActiveIndex((current) => Math.min(current + 1, suggestions.length - 1))
      return
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActiveIndex((current) => Math.max(current - 1, 0))
      return
    }

    if (event.key === 'Enter' || event.key === 'Tab') {
      event.preventDefault()
      const suggestion = suggestions[activeIndex]
      if (suggestion) {
        selectSuggestion(suggestion)
      }
      return
    }

    if (event.key === 'Escape') {
      event.preventDefault()
      setOpen(false)
    }
  }

  return (
    <div className="relative">
      <textarea
        {...textareaProps}
        ref={textareaRef}
        value={value}
        onChange={(event) => handleChange(event.target.value)}
        onFocus={handleFocus}
        onBlur={handleBlur}
        onKeyDown={handleKeyDown}
        onClick={(event) => scheduleSuggestions(value, event.currentTarget.selectionStart ?? value.length)}
        aria-autocomplete="list"
        aria-controls={open ? listboxId : undefined}
        aria-expanded={open}
        className={className}
      />

      {open && suggestions.length > 0 && (
        <ul
          id={listboxId}
          role="listbox"
          onMouseMove={handleListMouseMove}
          className="absolute z-20 mt-1 max-h-56 w-full overflow-auto rounded-md border border-gray-200 bg-white py-1 text-sm shadow-lg"
        >
          {suggestions.map((suggestion, index) => (
            <li
              key={`${suggestion.kind}-${suggestion.value}-${index}`}
              data-option-index={index}
              ref={(element) => {
                optionRefs.current[index] = element
              }}
              role="option"
              aria-selected={index === activeIndex}
            >
              <button
                type="button"
                tabIndex={-1}
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => selectSuggestion(suggestion)}
                className={`flex w-full items-start gap-2 px-3 py-2 text-left ${
                  index === activeIndex ? 'bg-brand-50 text-brand-900' : 'text-gray-900'
                }`}
              >
                <span className="shrink-0 rounded bg-gray-100 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-gray-600">
                  {suggestion.kind}
                </span>
                <span className="min-w-0 break-all">{suggestion.label}</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      {loading && open && (
        <p className="absolute right-2 top-2 text-xs text-gray-400">Loading suggestions...</p>
      )}
    </div>
  )
}
