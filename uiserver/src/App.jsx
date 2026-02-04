import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './pages/auth/Login';
import BuyerSignup from './pages/auth/BuyerSignup';
import SellerSignup from './pages/auth/SellerSignup';
import BuyerDashboard from './pages/BuyerDashboard';
import SellerDashboard from './pages/SellerDashboard';

function ProtectedRoute({ children, allowedRole }) {
    const { user } = useAuth();

    if (!user) {
        return <Navigate to="/login" />;
    }

    if (allowedRole && user.role !== allowedRole) {
        return <Navigate to={user.role === 'BUYER' ? '/buyer/dashboard' : '/seller/dashboard'} />;
    }

    return children;
}

function AppRoutes() {
    const { user } = useAuth();

    return (
        <Routes>
            <Route path="/login" element={user ? <Navigate to={user.role === 'BUYER' ? '/buyer/dashboard' : '/seller/dashboard'} /> : <Login />} />
            <Route path="/register/buyer" element={user ? <Navigate to="/buyer/dashboard" /> : <BuyerSignup />} />
            <Route path="/register/seller" element={user ? <Navigate to="/seller/dashboard" /> : <SellerSignup />} />
            
            <Route path="/buyer/dashboard" element={
                <ProtectedRoute allowedRole="BUYER">
                    <BuyerDashboard />
                </ProtectedRoute>
            } />
            
            <Route path="/seller/dashboard" element={
                <ProtectedRoute allowedRole="SELLER">
                    <SellerDashboard />
                </ProtectedRoute>
            } />

            <Route path="/" element={<Navigate to="/login" />} />
            <Route path="*" element={<Navigate to="/login" />} />
        </Routes>
    );
}

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <AppRoutes />
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;