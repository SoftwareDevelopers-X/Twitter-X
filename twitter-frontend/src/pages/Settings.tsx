import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { authService } from '../services/api';
import { useMutation } from '@tanstack/react-query';
import { Key, ShieldCheck, Loader2, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const passwordChangeSchema = z.object({
  oldPassword: z.string().min(8, 'Password must be at least 8 characters'),
  newPassword: z.string().min(8, 'New password must be at least 8 characters'),
  confirmPassword: z.string().min(8, 'Confirm password must be at least 8 characters'),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "New passwords don't match",
  path: ['confirmPassword'],
});

type PasswordChangeFormValues = z.infer<typeof passwordChangeSchema>;

const Settings: React.FC = () => {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PasswordChangeFormValues>({
    resolver: zodResolver(passwordChangeSchema),
  });

  const changePasswordMutation = useMutation({
    mutationFn: (data: Omit<PasswordChangeFormValues, 'confirmPassword'>) => 
      authService.changePassword(data),
    onSuccess: () => {
      reset();
      toast.success('Password updated successfully');
    },
    onError: (err: any) => {
      console.error(err);
      const errMsg = err.response?.data?.message || 'Failed to change password. Please check your current password.';
      toast.error(errMsg);
    }
  });

  const onSubmit = (data: PasswordChangeFormValues) => {
    changePasswordMutation.mutate({
      oldPassword: data.oldPassword,
      newPassword: data.newPassword,
    });
  };

  return (
    <div className="flex flex-col min-h-screen bg-black text-left">
      
      {/* Header */}
      <div className="sticky top-0 bg-black/85 backdrop-blur-md border-b border-twitter-dark-4 z-20 px-4 py-3">
        <h2 className="font-extrabold text-xl text-white">Settings</h2>
      </div>

      <div className="p-4 space-y-6 max-w-[500px]">
        <div className="flex items-center gap-2 text-twitter-blue">
          <Key className="w-5 h-5" />
          <h3 className="font-bold text-lg text-white">Security & Password</h3>
        </div>
        
        <p className="text-twitter-gray-1 text-sm leading-relaxed">
          Manage your account security. It is recommended to use a unique password that you don't use elsewhere.
        </p>

        {/* Change password Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 pt-2">
          
          {/* Old Password */}
          <div>
            <label className="block text-twitter-gray-1 text-xs font-bold uppercase mb-1.5 pl-1">Current Password</label>
            <input
              type="password"
              placeholder="Enter current password"
              {...register('oldPassword')}
              className={`w-full bg-transparent border ${
                errors.oldPassword ? 'border-red-500/50 focus:border-red-500' : 'border-twitter-dark-4 focus:border-twitter-blue'
              } rounded-xl px-4 py-3 text-white placeholder-twitter-gray-1 focus:outline-none focus:ring-1 focus:ring-transparent transition-all duration-200`}
            />
            {errors.oldPassword && (
              <p className="text-red-500 text-xs mt-1.5 pl-1 flex items-center gap-1">
                <AlertCircle className="w-3.5 h-3.5" />
                <span>{errors.oldPassword.message}</span>
              </p>
            )}
          </div>

          {/* New Password */}
          <div>
            <label className="block text-twitter-gray-1 text-xs font-bold uppercase mb-1.5 pl-1">New Password</label>
            <input
              type="password"
              placeholder="At least 8 characters"
              {...register('newPassword')}
              className={`w-full bg-transparent border ${
                errors.newPassword ? 'border-red-500/50 focus:border-red-500' : 'border-twitter-dark-4 focus:border-twitter-blue'
              } rounded-xl px-4 py-3 text-white placeholder-twitter-gray-1 focus:outline-none focus:ring-1 focus:ring-transparent transition-all duration-200`}
            />
            {errors.newPassword && (
              <p className="text-red-500 text-xs mt-1.5 pl-1 flex items-center gap-1">
                <AlertCircle className="w-3.5 h-3.5" />
                <span>{errors.newPassword.message}</span>
              </p>
            )}
          </div>

          {/* Confirm Password */}
          <div>
            <label className="block text-twitter-gray-1 text-xs font-bold uppercase mb-1.5 pl-1">Confirm New Password</label>
            <input
              type="password"
              placeholder="Re-enter your new password"
              {...register('confirmPassword')}
              className={`w-full bg-transparent border ${
                errors.confirmPassword ? 'border-red-500/50 focus:border-red-500' : 'border-twitter-dark-4 focus:border-twitter-blue'
              } rounded-xl px-4 py-3 text-white placeholder-twitter-gray-1 focus:outline-none focus:ring-1 focus:ring-transparent transition-all duration-200`}
            />
            {errors.confirmPassword && (
              <p className="text-red-500 text-xs mt-1.5 pl-1 flex items-center gap-1">
                <AlertCircle className="w-3.5 h-3.5" />
                <span>{errors.confirmPassword.message}</span>
              </p>
            )}
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={changePasswordMutation.isPending}
            className="px-6 py-2.5 bg-twitter-blue hover:bg-twitter-blue-hover text-white font-bold rounded-full text-sm transition-all duration-200 flex items-center gap-1.5 active:scale-95 disabled:active:scale-100 disabled:opacity-50"
          >
            {changePasswordMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
            <span>Update Password</span>
          </button>
        </form>

      </div>

    </div>
  );
};

export default Settings;
