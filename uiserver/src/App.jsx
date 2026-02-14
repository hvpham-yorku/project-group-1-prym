import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './pages/auth/Login';
import BuyerSignup from './pages/auth/BuyerSignup';
import SellerSignup from './pages/auth/SellerSignup';
import BuyerDashboard from './pages/BuyerDashboard';
import BuyerProfileSetup from './pages/BuyerProfileSetup';
import BuyerProfile from './pages/BuyerProfile';
import SellerDashboard from './pages/SellerDashboard';
import FarmListingsPage from './pages/FarmListingsPage';
import FarmListing from './pages/FarmListing';
import SellerProfileSetup from './pages/SellerProfileSetup';

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
            <Route path="/register/buyer" element={user ? <Navigate to="/buyer/profile-setup" /> : <BuyerSignup />} />
            <Route path="/register/seller" element={user ? <Navigate to="/seller/profile-setup" /> : <SellerSignup />} />
            
            <Route path="/buyer/dashboard" element={
                <ProtectedRoute allowedRole="BUYER">
                    <BuyerDashboard />
                </ProtectedRoute>
            } />
            
            <Route path="/buyer/profile-setup" element={
                <ProtectedRoute allowedRole="BUYER">
                    <BuyerProfileSetup />
                </ProtectedRoute>
            } />

            <Route path="/buyer/profile" element={
                <ProtectedRoute allowedRole="BUYER">
                    <BuyerProfile />
                </ProtectedRoute>
            } />

            <Route path="/seller/dashboard" element={
                <ProtectedRoute allowedRole="SELLER">
                    <SellerDashboard />
                </ProtectedRoute>
            } />
			
			<Route path="/seller/profile-setup" element={
			    <ProtectedRoute allowedRole="SELLER">
			        <SellerProfileSetup />
			    </ProtectedRoute>
			} />


            <Route path="/" element={<Navigate to="/login" />} />
            <Route path="*" element={<Navigate to="/login" />} />
			
			<Route path="/farmlistingspage" element={<FarmListingsPage />} />
			<Route path="/farmlistingspage/farmlisting" element={<FarmListing />} />
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