import { createBrowserRouter, Navigate } from "react-router-dom";
import { App } from "./App";
import { BoostingHallPage } from "./pages/BoostingHallPage";
import { BoostingOrderPage } from "./pages/BoostingOrderPage";
import { BoostingOrdersPage } from "./pages/BoostingOrdersPage";
import { DistributionInvitePage } from "./pages/DistributionInvitePage";
import { GunCodePage } from "./pages/GunCodePage";
import { HomePage } from "./pages/HomePage";
import { ImChatPage } from "./pages/ImChatPage";
import { MyListingsPage } from "./pages/MyListingsPage";
import { ProfilePage } from "./pages/ProfilePage";
import { PublishPage } from "./pages/PublishPage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      { index: true, element: <HomePage marketType="rental" /> },
      { path: "trade", element: <HomePage marketType="trade" /> },
      { path: "invite/:inviteCode", element: <DistributionInvitePage /> },
      { path: "gun-code", element: <GunCodePage /> },
      { path: "boosting", element: <BoostingHallPage /> },
      { path: "boosting/:serviceNo/order", element: <BoostingOrderPage /> },
      { path: "boosting/orders", element: <BoostingOrdersPage /> },
      { path: "im/:conversationNo", element: <ImChatPage /> },
      { path: "support/im", element: <Navigate to="/profile?tab=messages" replace /> },
      { path: "profile", element: <ProfilePage /> },
      { path: "publish", element: <PublishPage /> },
      { path: "publish/mine", element: <MyListingsPage /> },
    ],
  },
]);
