type BrandLogoProps = {
  className?: string
}

export function BrandLogo({ className }: BrandLogoProps) {
  return (
    <img
      alt=""
      aria-hidden="true"
      className={className}
      draggable={false}
      src={`${import.meta.env.BASE_URL}fziot-logo.png`}
    />
  )
}
