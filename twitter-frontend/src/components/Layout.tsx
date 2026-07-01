<<<<<<< HEAD
import React from 'react';
import { Outlet } from 'react-router-dom';
=======
import { Outlet, useLocation } from 'react-router-dom';
>>>>>>> 405d85f (resolved bugs on chat-service)
import Sidebar from './Sidebar';
import RightSidebar from './RightSidebar';
import { useAuthStore } from '../store/authStore';
import TweetBox from './TweetBox';
import { X } from 'lucide-react';

const Layout: React.FC = () => {
  const { isComposerOpen, setComposerOpen } = useAuthStore();
<<<<<<< HEAD
=======
  const location = useLocation();

  const isMessagesPage = location.pathname.startsWith('/messages');
>>>>>>> 405d85f (resolved bugs on chat-service)

  return (
    <div className="min-h-screen bg-black text-white flex justify-center selection:bg-twitter-blue/30">
      <div className="flex w-full max-w-[1250px] relative px-0 sm:px-4">
        
        {/* Left Navigation Sidebar */}
        <header className="w-[68px] xl:w-[275px] h-screen sticky top-0 flex-shrink-0 z-30">
          <Sidebar />
        </header>

        {/* Center Main Content Scroll */}
<<<<<<< HEAD
        <main className="flex-grow border-r border-l border-twitter-dark-4 pb-20 sm:pb-0 max-w-[600px] min-h-screen">
=======
        <main className={`flex-grow border-r border-l border-twitter-dark-4 pb-20 sm:pb-0 min-h-screen ${
          isMessagesPage ? 'max-w-none' : 'max-w-[600px]'
        }`}>
>>>>>>> 405d85f (resolved bugs on chat-service)
          <Outlet />
        </main>

        {/* Right Info Sidebar (Trends, search) */}
        <aside className="hidden lg:block w-[290px] xl:w-[350px] h-screen sticky top-0 pl-6 flex-shrink-0 z-30 overflow-y-auto">
          <RightSidebar />
        </aside>

      </div>


      {/* Tweet Composer Modal Dialog */}
      {isComposerOpen && (
        <div 
          onClick={() => setComposerOpen(false)}
          className="fixed inset-0 bg-neutral-900/40 backdrop-blur-sm z-50 flex items-start justify-center pt-[10%] px-4"
        >
          <div 
            onClick={(e) => e.stopPropagation()}
            className="bg-black border border-twitter-dark-4 rounded-2xl w-full max-w-[600px] overflow-hidden shadow-2xl animate-fade-in text-left flex flex-col"
          >
            {/* Modal Header */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-twitter-dark-4">
              <div className="flex items-center gap-3">
                <button 
                  onClick={() => setComposerOpen(false)}
                  className="p-1.5 hover:bg-white/10 rounded-full transition-colors duration-200 text-white"
                >
                  <X className="w-5 h-5" />
                </button>
                <span className="font-extrabold text-white text-lg">Compose Post</span>
              </div>
            </div>
            
            {/* Modal Body */}
            <div className="overflow-y-auto">
              <TweetBox placeholder="What's happening?!" onSuccess={() => setComposerOpen(false)} />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Layout;
