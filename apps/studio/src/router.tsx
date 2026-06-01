import { createBrowserRouter } from "react-router-dom";
import { ImChatPage } from "../../web/src/pages/ImChatPage";
import { MyListingsPage } from "../../web/src/pages/MyListingsPage";
import { PublishPage } from "../../web/src/pages/PublishPage";
import { StudioApp } from "./pages/StudioApp";

export const router = createBrowserRouter(
  [
    { path: "/", element: <StudioApp /> },
    { path: "/publish", element: <PublishPage /> },
    { path: "/publish/mine", element: <MyListingsPage /> },
    { path: "/im/:conversationNo", element: <ImChatPage /> },
  ],
  { basename: "/studio" },
);
