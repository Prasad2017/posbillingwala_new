import Seo from '../common/Seo'
import Loader from '../common/Loader'
import ErrorState from '../common/ErrorState'
import { useApi } from '../../hooks/useApi'
import { getPageContent } from '../../services/websiteService'
import { formatDate } from '../../utils/helpers'
import Button from '../common/Button'
import './LegalPage.css'

export default function LegalPage({
  slug,
  fallbackTitle,
  path,
  description,
  lead,
}) {
  const { data, loading, error, refetch } = useApi(() => getPageContent(slug), [slug])

  return (
    <>
      <Seo title={data?.title || fallbackTitle} description={description} path={path} />
      <main className="legal-page">
        <div className="container">
          <article className="legal-card">
            <span className="page-kicker">LEGAL</span>
            <h1>{data?.title || fallbackTitle}</h1>
            {lead && <p className="legal-lead">{lead}</p>}

            {loading && <Loader label="Loading content…" />}
            {error && (
              <ErrorState title="Unable to load page" message={error} onRetry={refetch} />
            )}
            {!loading && !error && data && (
              <div
                className="legal-body"
                dangerouslySetInnerHTML={{ __html: data.body_html || '' }}
              />
            )}

            {data?.updated_at && (
              <p className="legal-updated">Last Updated: {formatDate(data.updated_at)}</p>
            )}

            <div className="legal-actions">
              <Button to="/contact" variant="primary">
                Contact Us
              </Button>
            </div>
          </article>
        </div>
      </main>
    </>
  )
}
