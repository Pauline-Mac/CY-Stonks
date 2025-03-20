import { CircularProgress } from '@mui/material';

export default function LoadingScreen() {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100">
      <h1 className="text-4xl font-bold text-gray-900 mb-4">CY Stonks</h1>
      <span className="text-blue-700">
        By Pauline, Jules & Donovan
      </span>
      <span className="text-blue-700">
        For Ms. Djaouda
      </span>
      <br />
      <CircularProgress className="mb-4" />
    </div>
  );
}
