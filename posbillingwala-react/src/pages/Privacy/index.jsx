import LegalPage from '../../components/common/LegalPage'
import { CMS_SLUGS } from '../../utils/constants'

export default function Privacy() {
  return (
    <LegalPage
      slug={CMS_SLUGS.privacy}
      fallbackTitle="Privacy Policy"
      path="/privacy-policy"
      description="Read the POS Billingwala privacy policy."
      lead="How we collect, use and protect information related to our website and services."
    />
  )
}
