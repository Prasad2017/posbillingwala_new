import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

const DARK_HERO_ROUTES = ['/', '/download-app']

export default function HeaderTheme() {
  const { pathname } = useLocation()

  useEffect(() => {
    const solid = !DARK_HERO_ROUTES.includes(pathname)
    document.body.setAttribute('data-header', solid ? 'solid' : 'transparent')
  }, [pathname])

  return null
}
