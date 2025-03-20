import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import LandingPage from 'components/pages//LandingPage.tsx';

interface LandingPageProps {
  demo: boolean;
}

const landingPageDemoProps: LandingPageProps = { demo: true };
const landingPageProdProps = { demo: false };

export default function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<LandingPage {...landingPageDemoProps} />} />
        <Route path="/prod" element={<LandingPage {...landingPageProdProps} />} />
      </Routes>
    </Router>
  );
}