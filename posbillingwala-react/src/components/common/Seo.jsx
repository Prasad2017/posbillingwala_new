import { Helmet } from 'react-helmet-async'
import { buildSeo } from '../../utils/seo'

export default function Seo({ title, description, path, image }) {
  const seo = buildSeo({ title, description, path, image })
  return (
    <Helmet>
      <title>{seo.title}</title>
      <meta name="description" content={seo.description} />
      <link rel="canonical" href={seo.canonical} />
      <meta property="og:title" content={seo.ogTitle} />
      <meta property="og:description" content={seo.ogDescription} />
      <meta property="og:url" content={seo.ogUrl} />
      <meta property="og:type" content="website" />
      <meta property="og:image" content={seo.ogImage} />
      <meta name="twitter:card" content="summary_large_image" />
    </Helmet>
  )
}
