import LegalPage from '../../components/common/LegalPage'
import { CMS_SLUGS } from '../../utils/constants'

export default function Refund() {
  return (
    <LegalPage
      slug={CMS_SLUGS.refund}
      fallbackTitle="Refund & Renewal Policy"
      path="/refund-policy"
      description="Read the POS Billingwala refund and renewal policy."
      lead="Information about renewals, refunds and related policy details."
    />
  )
}
