import LegalPage from '../../components/common/LegalPage'
import { CMS_SLUGS } from '../../utils/constants'

export default function Terms() {
  return (
    <LegalPage
      slug={CMS_SLUGS.terms}
      fallbackTitle="Terms & Conditions"
      path="/terms-conditions"
      description="Read the POS Billingwala terms and conditions."
      lead="The terms that govern use of POS Billingwala software, website and related services."
    />
  )
}
