interface TabButtonProps {
  active: boolean
  onClick: () => void
  label: string
}

export function TabButton({ active, onClick, label }: TabButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`border-b-2 px-1 py-3 text-sm font-medium transition ${
        active
          ? 'border-brand-600 text-brand-600'
          : 'border-transparent text-gray-500 hover:border-gray-200 hover:text-gray-600'
      }`}
    >
      {label}
    </button>
  )
}
