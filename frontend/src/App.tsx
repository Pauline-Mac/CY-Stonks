import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import LandingPage from 'components/pages//LandingPage.tsx';
import LoginForm from 'components/pages/LoginForm.tsx';
import RegisterForm from 'components/pages/RegisterForm.tsx';
import TransactionPage from 'components/pages/TransactionPage.tsx';

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
          <Route path="/login" element={<LoginForm />} />
          <Route path="/register" element={<RegisterForm />} />
          <Route path="/transaction" element={<TransactionPage />} />

      </Routes>
    </Router>
  );
}