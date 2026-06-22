import React from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useAuthStore } from '../store/authStore';
import { Loader2, AlertCircle, ArrowRight } from 'lucide-react';
import toast from 'react-hot-toast';

const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

const Login: React.FC = () => {
  const navigate = useNavigate();
  const { login, isLoading } = useAuthStore();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormValues) => {
    try {
      await login(data);
      toast.success('Welcome back!');
      navigate('/');
    } catch (err: any) {
      console.error(err);
      const errMsg = err.response?.data?.message || 'Login failed. Please check your credentials.';
      toast.error(errMsg);
    }
  };

  return (
    <div className="min-h-screen bg-black flex items-center justify-center px-4 relative overflow-hidden">
      
      {/* Decorative gradient glowing backgrounds */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-twitter-blue/10 rounded-full blur-[100px] pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-purple-500/5 rounded-full blur-[120px] pointer-events-none" />

      <div className="w-full max-w-[440px] bg-twitter-dark-2 border border-twitter-dark-4 p-8 rounded-2xl shadow-2xl relative z-10">
        
        {/* Logo */}
        <div className="flex flex-col items-center mb-6">
          <svg viewBox="0 0 24 24" aria-hidden="true" className="w-10 h-10 fill-current text-twitter-blue mb-3">
            <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"></path>
          </svg>
          <h1 className="text-2xl font-black text-white tracking-tight">Sign in to X-Clone</h1>
          <p className="text-twitter-gray-1 text-sm mt-1">Connect with your world</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          
          {/* Email */}
          <div>
            <label className="block text-twitter-gray-1 text-xs font-bold uppercase mb-1.5 pl-1">Email Address</label>
            <input
              type="email"
              placeholder="Enter your email"
              {...register('email')}
              className={`w-full bg-black/50 border ${
                errors.email ? 'border-red-500/50 focus:border-red-500' : 'border-twitter-dark-4 focus:border-twitter-blue'
              } rounded-xl px-4 py-3 text-white text-[15px] placeholder-twitter-gray-1 focus:outline-none focus:ring-1 focus:ring-transparent transition-all duration-200`}
            />
            {errors.email && (
              <p className="text-red-500 text-xs mt-1.5 pl-1 flex items-center gap-1">
                <AlertCircle className="w-3.5 h-3.5" />
                <span>{errors.email.message}</span>
              </p>
            )}
          </div>

          {/* Password */}
          <div>
            <div className="flex justify-between items-center mb-1.5 pl-1">
              <label className="block text-twitter-gray-1 text-xs font-bold uppercase">Password</label>
              <Link 
                to="/forgot-password" 
                className="text-twitter-blue hover:underline text-xs"
              >
                Forgot password?
              </Link>
            </div>
            <input
              type="password"
              placeholder="Enter your password"
              {...register('password')}
              className={`w-full bg-black/50 border ${
                errors.password ? 'border-red-500/50 focus:border-red-500' : 'border-twitter-dark-4 focus:border-twitter-blue'
              } rounded-xl px-4 py-3 text-white text-[15px] placeholder-twitter-gray-1 focus:outline-none focus:ring-1 focus:ring-transparent transition-all duration-200`}
            />
            {errors.password && (
              <p className="text-red-500 text-xs mt-1.5 pl-1 flex items-center gap-1">
                <AlertCircle className="w-3.5 h-3.5" />
                <span>{errors.password.message}</span>
              </p>
            )}
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-white hover:bg-neutral-200 text-black font-extrabold py-3.5 rounded-full mt-2 transition-all duration-200 flex items-center justify-center gap-2 disabled:bg-neutral-500 disabled:text-neutral-300 disabled:cursor-not-allowed text-[15px] active:scale-95 disabled:active:scale-100 shadow-lg shadow-white/5"
          >
            {isLoading ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <>
                <span>Sign In</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        {/* Separator / Redirect */}
        <div className="mt-8 pt-6 border-t border-twitter-dark-4 text-center">
          <p className="text-twitter-gray-1 text-sm">
            Don't have an account?{' '}
            <Link to="/register" className="text-twitter-blue hover:underline font-bold">
              Sign up
            </Link>
          </p>
        </div>

      </div>
    </div>
  );
};

export default Login;
