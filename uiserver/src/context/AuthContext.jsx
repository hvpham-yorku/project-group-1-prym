import { createContext, useContext, useState, useEffect } from 'react';
import { getCurrentUser } from '../api/auth';

//global auth context that holds the logged-in user object.
//wraps the whole app so any component can call useAuth() to
//get the user, save updated user data, or clear it on logout.
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Check if user is already logged in (has valid session cookie)
        getCurrentUser()
            .then((userData) => {
                setUser(userData);
            })
            .catch(() => {
                // Not logged in or session expired
                setUser(null);
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    const saveUser = (userData) => {
        setUser(userData);
    };

    const clearUser = () => {
        setUser(null);
    };

    if (loading) {
        return (
            <div style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                height: '100vh',
                backgroundColor: '#f5f5f0'
            }}>
                Loading...
            </div>
        );
    }

    return (
        <AuthContext.Provider value={{ user, saveUser, clearUser }}>
            {children}
        </AuthContext.Provider>
    );
}

//convenience hook so you don't have to import useContext and AuthContext separately
export function useAuth() {
    return useContext(AuthContext);
}