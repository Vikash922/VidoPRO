import { useState } from "react";
import {
  Home,
  Search,
  Settings,
  Plus,
  Waypoints,
  Camera,
  Sparkles,
  LayoutTemplate,
  AlignLeft,
  MoreVertical,
  Scissors,
  Inbox,
  User,
  Zap,
} from "lucide-react";

const projects = [
  {
    id: 1,
    title: "My Edit 12",
    meta: "00:32 · 1080P",
    time: "2h ago",
    thumb:
      "https://images.pexels.com/photos/3863218/pexels-photo-3863218.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=200&w=280",
  },
  {
    id: 2,
    title: "Travel Vlog",
    meta: "01:45 · 1080P",
    time: "5h ago",
    thumb:
      "https://images.pexels.com/photos/20344919/pexels-photo-20344919.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=200&w=280",
  },
  {
    id: 3,
    title: "Cinematic",
    meta: "00:58 · 1080P",
    time: "1d ago",
    thumb:
      "https://images.pexels.com/photos/14169534/pexels-photo-14169534.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=200&w=280",
  },
  {
    id: 4,
    title: "Reels",
    meta: "00:21 · 1080P",
    time: "2d ago",
    thumb:
      "https://images.pexels.com/photos/19779565/pexels-photo-19779565.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=200&w=280",
  },
];

const templates = [
  {
    id: 1,
    name: "Cinematic",
    duration: "00:15",
    img: "https://images.pexels.com/photos/14169534/pexels-photo-14169534.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=320&w=240",
  },
  {
    id: 2,
    name: "Travel",
    duration: "00:30",
    img: "https://images.pexels.com/photos/3863218/pexels-photo-3863218.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=320&w=240",
  },
  {
    id: 3,
    name: "Sunset",
    duration: "00:20",
    img: "https://images.pexels.com/photos/29857601/pexels-photo-29857601.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=320&w=240",
  },
  {
    id: 4,
    name: "Vlog",
    duration: "01:00",
    img: "https://images.pexels.com/photos/28492053/pexels-photo-28492053.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=320&w=240",
  },
];

const quickActions = [
  { label: "AutoCut", icon: Waypoints },
  { label: "Camera", icon: Camera },
  { label: "AI Tools", icon: Sparkles },
  { label: "Templates", icon: LayoutTemplate },
];

const navItems = [
  { label: "Home", icon: Home },
  { label: "Edit", icon: Scissors },
  { label: "Template", icon: LayoutTemplate },
  { label: "Inbox", icon: Inbox },
  { label: "Me", icon: User },
];

export default function App() {
  const [activeNav, setActiveNav] = useState(0);
  const [activeTab, setActiveTab] = useState<"projects" | "templates">("projects");

  return (
    <div
      className="min-h-screen flex items-center justify-center"
      style={{ background: "#d5e6f5" }}
    >
      {/* Phone Frame */}
      <div
        className="relative flex flex-col overflow-hidden"
        style={{
          width: 375,
          height: 820,
          background: "#f8faff",
          borderRadius: 44,
          boxShadow:
            "0 0 0 9px #ffffff, 0 0 0 11px #bcd8ee, 0 32px 70px rgba(70,120,170,0.28)",
        }}
      >
        {/* Status Bar */}
        <div className="flex items-center justify-between px-7 pt-4 pb-1">
          <span className="text-xs font-semibold" style={{ color: "#1a3a5c" }}>
            9:41
          </span>
          <div
            style={{
              width: 80,
              height: 20,
              background: "#e8f2fc",
              borderRadius: 12,
            }}
          />
          <div className="flex items-center gap-1.5">
            <svg width="15" height="11" viewBox="0 0 16 12" fill="none">
              <rect x="0" y="5" width="3" height="7" rx="1" fill="#1a3a5c" />
              <rect x="4.5" y="3" width="3" height="9" rx="1" fill="#1a3a5c" />
              <rect x="9" y="1" width="3" height="11" rx="1" fill="#1a3a5c" />
              <rect x="13.5" y="0" width="2.5" height="12" rx="1" fill="#1a3a5c" />
            </svg>
            <svg width="14" height="11" viewBox="0 0 15 12" fill="none">
              <path d="M7.5 2C9.8 2 11.8 3.1 13.1 4.8L14.5 3.3C12.8 1.3 10.3 0 7.5 0C4.7 0 2.2 1.3 0.5 3.3L1.9 4.8C3.2 3.1 5.2 2 7.5 2Z" fill="#1a3a5c" />
              <path d="M7.5 5C9 5 10.3 5.7 11.2 6.8L12.6 5.3C11.3 3.9 9.5 3 7.5 3C5.5 3 3.7 3.9 2.4 5.3L3.8 6.8C4.7 5.7 6 5 7.5 5Z" fill="#1a3a5c" />
              <circle cx="7.5" cy="10" r="2" fill="#1a3a5c" />
            </svg>
            <svg width="24" height="12" viewBox="0 0 25 12" fill="none">
              <rect x="0.5" y="0.5" width="21" height="11" rx="3.5" stroke="#1a3a5c" strokeOpacity="0.4" />
              <rect x="2" y="2" width="16" height="8" rx="2" fill="#1a3a5c" />
              <path d="M23 4.5V7.5C23.8 7.2 24.5 6.5 24.5 6C24.5 5.5 23.8 4.8 23 4.5Z" fill="#1a3a5c" fillOpacity="0.4" />
            </svg>
          </div>
        </div>

        {/* Header */}
        <div className="flex items-center justify-between px-5 pt-4 pb-1">
          <div className="flex items-center gap-2">
            <div
              className="flex items-center justify-center rounded-lg"
              style={{ width: 26, height: 26, background: "#4a90c8" }}
            >
              <Zap size={14} color="#ffffff" fill="#ffffff" />
            </div>
            <span className="text-lg font-bold" style={{ color: "#1a3a5c" }}>
              EditPro
            </span>
          </div>
          <div className="flex items-center gap-4">
            <Search size={19} color="#1a3a5c" strokeWidth={1.8} />
            <Settings size={19} color="#1a3a5c" strokeWidth={1.8} />
          </div>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto" style={{ paddingBottom: 84 }}>
          {/* New Project Button */}
          <div className="px-5 mt-3">
            <button
              className="w-full flex items-center justify-center gap-2 rounded-xl py-3.5 text-sm font-semibold"
              style={{
                background: "#4a90c8",
                color: "#ffffff",
                boxShadow: "0 6px 16px rgba(74,144,200,0.35)",
              }}
            >
              <Plus size={17} strokeWidth={2.4} />
              New Project
            </button>
          </div>

          {/* Quick Actions */}
          <div className="grid grid-cols-4 gap-2.5 px-5 mt-4">
            {quickActions.map((action) => (
              <button
                key={action.label}
                className="flex flex-col items-center justify-center gap-1.5 rounded-xl py-3"
                style={{
                  background: "#ffffff",
                  border: "1.5px solid #dcebf7",
                }}
              >
                <action.icon size={19} color="#4a90c8" strokeWidth={1.8} />
                <span
                  className="font-medium"
                  style={{ fontSize: 10.5, color: "#1a3a5c" }}
                >
                  {action.label}
                </span>
              </button>
            ))}
          </div>

          {/* Tabs */}
          <div className="flex items-center justify-between px-5 mt-5">
            <div className="flex items-center gap-6">
              {(["projects", "templates"] as const).map((tab) => (
                <button
                  key={tab}
                  className="relative pb-1.5"
                  onClick={() => setActiveTab(tab)}
                >
                  <span
                    className="text-sm"
                    style={{
                      color: activeTab === tab ? "#1a3a5c" : "#8ab4d4",
                      fontWeight: activeTab === tab ? 700 : 500,
                    }}
                  >
                    {tab === "projects" ? "Projects" : "Templates"}
                  </span>
                  {activeTab === tab && (
                    <span
                      className="absolute bottom-0 left-1/2 -translate-x-1/2 rounded-full"
                      style={{ width: 18, height: 3, background: "#4a90c8" }}
                    />
                  )}
                </button>
              ))}
            </div>
            <AlignLeft size={18} color="#1a3a5c" strokeWidth={2} />
          </div>

          {/* Projects List */}
          {activeTab === "projects" ? (
            <div className="flex flex-col gap-1 px-5 mt-1">
              {projects.map((p) => (
                <div
                  key={p.id}
                  className="flex items-center gap-3 py-2.5"
                  style={{ borderBottom: "1px solid #e8f1f9" }}
                >
                  <img
                    src={p.thumb}
                    alt={p.title}
                    className="rounded-lg object-cover flex-shrink-0"
                    style={{ width: 74, height: 52 }}
                  />
                  <div className="flex-1 min-w-0">
                    <p
                      className="font-semibold truncate"
                      style={{ fontSize: 14, color: "#1a3a5c" }}
                    >
                      {p.title}
                    </p>
                    <p style={{ fontSize: 11, color: "#8ab4d4" }}>{p.meta}</p>
                    <p style={{ fontSize: 11, color: "#aac4dc" }}>{p.time}</p>
                  </div>
                  <MoreVertical size={17} color="#8ab4d4" />
                </div>
              ))}
            </div>
          ) : (
            /* Templates Grid */
            <div className="grid grid-cols-2 gap-3 px-5 mt-3">
              {templates.map((t) => (
                <button
                  key={t.id}
                  className="text-left rounded-xl overflow-hidden"
                  style={{
                    background: "#ffffff",
                    border: "1.5px solid #dcebf7",
                  }}
                >
                  <img
                    src={t.img}
                    alt={t.name}
                    className="w-full object-cover"
                    style={{ height: 100 }}
                  />
                  <div className="px-2.5 py-2 flex items-center justify-between">
                    <span
                      className="text-xs font-semibold"
                      style={{ color: "#1a3a5c" }}
                    >
                      {t.name}
                    </span>
                    <span className="text-xs" style={{ color: "#8ab4d4" }}>
                      {t.duration}
                    </span>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Bottom Navigation */}
        <div
          className="absolute bottom-0 left-0 right-0 flex items-center justify-around px-3 pt-2.5 pb-6"
          style={{
            background: "#ffffff",
            borderTop: "1.5px solid #dcebf7",
          }}
        >
          {navItems.map((item, idx) => (
            <button
              key={item.label}
              className="flex flex-col items-center gap-1"
              onClick={() => setActiveNav(idx)}
              style={{ minWidth: 52 }}
            >
              <item.icon
                size={21}
                color={activeNav === idx ? "#4a90c8" : "#b8cfe4"}
                strokeWidth={activeNav === idx ? 2.2 : 1.7}
              />
              <span
                style={{
                  fontSize: 10,
                  color: activeNav === idx ? "#4a90c8" : "#b8cfe4",
                  fontWeight: activeNav === idx ? 600 : 400,
                }}
              >
                {item.label}
              </span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
