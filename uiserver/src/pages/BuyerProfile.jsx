
/*Since everything from BuyerProfile was merged into BuyerDashboard, this page just redirects immediately. This is needed so any existing links to /buyer/profile don't 404.*/
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

function BuyerProfile() {
  const navigate = useNavigate();

  useEffect(() => {
    navigate("/buyer/dashboard", { replace: true });
  }, []);

  return null;
}

export default BuyerProfile;
