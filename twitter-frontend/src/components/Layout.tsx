import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import RightSidebar from './RightSidebar';

const Layout: React.FC = () => {
  return (
    <div className="min-h-screen bg-black text-white flex justify-center selection:bg-twitter-blue/30">
      <div className="flex w-full max-w-[1250px] relative px-0 sm:px-4">
        
        {/* Left Navigation Sidebar */}
        <header className="w-[68px] xl:w-[275px] h-screen sticky top-0 flex-shrink-0 z-30">
          <Sidebar />
        </header>

        {/* Center Main Content Scroll */}
        <main className="flex-grow max-w-[600px] min-h-screen border-r border-l border-twitter-dark-4 pb-20 sm:pb-0">
          <Outlet />
        </main>

        {/* Right Info Sidebar (Trends, search) */}
        <aside className="hidden lg:block w-[290px] xl:w-[350px] h-screen sticky top-0 pl-6 flex-shrink-0 z-30 overflow-y-auto">
          <RightSidebar />
        </aside>

      </div>
    </div>
  );
};

export default Layout;
