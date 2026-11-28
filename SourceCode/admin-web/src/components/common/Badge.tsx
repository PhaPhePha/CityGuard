import React from 'react';

interface BadgeProps {
  children: React.ReactNode;
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info';
  color?: string; // Optional custom hex background
  size?: 'sm' | 'md';
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'default',
  color,
  size = 'sm',
  className = ''
}) => {
  const variantStyles = {
    default: 'bg-slate-100 text-slate-800 border-slate-200',
    success: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    warning: 'bg-amber-50 text-amber-700 border-amber-200',
    danger: 'bg-rose-50 text-rose-700 border-rose-200',
    info: 'bg-sky-50 text-sky-700 border-sky-200'
  };

  const sizeStyles = {
    sm: 'px-2.5 py-0.5 text-xs',
    md: 'px-3 py-1 text-xs font-semibold'
  };

  const style = color ? { backgroundColor: `${color}15`, color, borderColor: `${color}30` } : undefined;

  return (
    <span
      style={style}
      className={`inline-flex items-center rounded-full font-medium border ${sizeStyles[size]} ${
        color ? '' : variantStyles[variant]
      } ${className}`}
    >
      {children}
    </span>
  );
};
