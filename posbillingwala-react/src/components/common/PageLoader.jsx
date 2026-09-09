import Loader from './Loader'
import './PageLoader.css'

export default function PageLoader() {
  return (
    <div className="page-loader">
      <Loader label="Loading page…" />
    </div>
  )
}
